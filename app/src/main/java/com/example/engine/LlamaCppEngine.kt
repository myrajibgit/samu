package com.example.engine

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.example.model.ModelItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nehuatl.llamacpp.LlamaHelper
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * Real on-device inference engine: runs the downloaded GGUF weights directly on the
 * phone's CPU via llama.cpp native bindings. No network, no API key.
 *
 * Usage:
 *  1. [loadIntoRam] once per model (loads weights + KV cache, switching models frees the old one).
 *  2. [generateStream] for each chat turn — streams tokens as the model predicts them.
 *  3. [stopGeneration] mid-stream, [release] to free RAM, [shutdown] on ViewModel teardown.
 */
class LlamaCppEngine(private val contentResolver: ContentResolver) {

    data class LoadedModel(
        val modelId: String,
        val name: String,
        val family: String,
        val contextLength: Int,
        val filePath: String
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val engineFlow = MutableSharedFlow<LlamaHelper.LLMEvent>(
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val helper by lazy { LlamaHelper(contentResolver, scope, engineFlow) }

    private val loadedRef = AtomicReference<LoadedModel?>(null)

    private val _loadedModel = MutableStateFlow<LoadedModel?>(null)
    val loadedModel: StateFlow<LoadedModel?> = _loadedModel.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Last failure reported by the native layer (load or generation).
     * Surfaced in the UI as a banner so failures are never silent.
     */
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun clearError() {
        _lastError.value = null
    }

    fun isModelLoaded(modelId: String): Boolean = loadedRef.get()?.modelId == modelId

    /**
     * Loads a GGUF file into RAM. Loading another model releases the previous one.
     * Suspend + bounded by [LOAD_TIMEOUT_MS]. Returns null on failure.
     */
    suspend fun loadIntoRam(model: ModelItem): LoadedModel? = withContext(Dispatchers.IO) {
        val file = model.localFilePath?.let { File(it) }
        if (file == null || !file.exists() || file.length() < 1024) {
            Log.e(TAG, "Model file missing or too small: ${model.localFilePath}")
            _lastError.value = "${model.name} isn't downloaded on this device yet."
            return@withContext null
        }
        loadedRef.get()?.let { if (it.modelId == model.id) return@withContext it }

        _isLoading.value = true
        _lastError.value = null
        try {
            release()

            val ctxLen = model.contextLength.coerceIn(512, 8192)
            val result = AtomicReference<LoadedModel?>(null)
            val error = AtomicReference<String?>(null)

            val collector = scope.launch {
                engineFlow.collect { event ->
                    when (event) {
                        is LlamaHelper.LLMEvent.Loaded -> result.set(
                            LoadedModel(
                                modelId = model.id,
                                name = model.name,
                                family = model.family,
                                contextLength = ctxLen,
                                filePath = file.absolutePath
                            )
                        )
                        is LlamaHelper.LLMEvent.Error -> if (error.get() == null) error.set(event.message)
                        else -> Unit
                    }
                }
            }

            helper.load(path = Uri.fromFile(file).toString(), contextLength = ctxLen) { }

            val deadline = System.currentTimeMillis() + LOAD_TIMEOUT_MS
            while (System.currentTimeMillis() < deadline) {
                if (result.get() != null || error.get() != null) break
                Thread.sleep(50)
            }
            collector.cancel()

            error.get()?.let { err ->
                Log.e(TAG, "Model load failed: $err")
                _lastError.value = "Couldn't load ${model.name}: $err"
                release()
                return@withContext null
            }

            val loaded = result.get()
            if (loaded == null) {
                Log.e(TAG, "Model load timed out after ${LOAD_TIMEOUT_MS / 1000}s")
                _lastError.value =
                    "Timed out after ${LOAD_TIMEOUT_MS / 1000}s while loading ${model.name} " +
                        "(${file.length() / (1024 * 1024)} MB). Free up RAM or pick a smaller model."
                release()
                return@withContext null
            }

            loadedRef.set(loaded)
            _loadedModel.value = loaded
            Log.i(TAG, "Model loaded into RAM: ${loaded.name} (ctx=$ctxLen)")
            loaded
        } catch (ce: kotlinx.coroutines.CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.e(TAG, "Exception during model load", e)
            _lastError.value = "Couldn't load ${model.name}: ${e.message ?: "unknown native error"}"
            release()
            null
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Streams a completion for the given chat history from the loaded model.
     * The prompt is rendered with the model family's chat template by [PromptFormatter].
     * Cancelling collection stops the native generation.
     */
    fun generateStream(
        history: List<Pair<String, String>>,
        systemPrompt: String,
        temperature: Float,
        topP: Float
    ): Flow<InferenceChunk> = channelFlow {
        val loaded = loadedRef.get()
            ?: throw IllegalStateException(
                "No model loaded into RAM. Download a model in the Models tab, then tap 'Load in RAM' (or just start chatting to auto-load it)."
            )

        // DeepSeek R1 distills emit reasoning blocks delimited by the thinking markers;
        // key off the model id so the UI can route that text into the collapsible
        // "thinking" section instead of the final answer.
        val isReasoningModel = loaded.modelId.contains("deepseek", ignoreCase = true)
        val templateFamily = if (isReasoningModel) "deepseek" else loaded.family
        val prompt = PromptFormatter.format(templateFamily, history, systemPrompt)

        val startMs = System.currentTimeMillis()

        // DeepSeek's template opens the reply inside the thinking block, so reasoning
        // models start there and leave it when the closing marker arrives.
        var insideThinking = isReasoningModel

        // Completes when the native generation finishes (or immediately if already done).
        val finished = CompletableDeferred<Unit>()

        val collector = launch {
            engineFlow.collect { event ->
                when (event) {
                    is LlamaHelper.LLMEvent.Ongoing -> {
                        var visible: String? = event.word
                        if (isReasoningModel) {
                            when (classifyThinkMarker(event.word)) {
                                ThinkMarker.OPEN -> {
                                    insideThinking = true
                                    visible = stripThinkMarker(event.word)
                                }
                                ThinkMarker.CLOSE -> {
                                    insideThinking = false
                                    visible = stripThinkMarker(event.word)
                                }
                                ThinkMarker.NONE -> Unit
                            }
                        }
                        if (visible != null) {
                            val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(1)
                            val tps = if (event.tokenCount > 0) event.tokenCount * 1000f / elapsed else 0f
                            send(
                                InferenceChunk(
                                    token = visible,
                                    isThinking = isReasoningModel && insideThinking,
                                    isDone = false,
                                    tokensGenerated = event.tokenCount,
                                    tokensPerSecond = tps,
                                    generationTimeMs = elapsed
                                )
                            )
                        }
                    }
                    is LlamaHelper.LLMEvent.Done -> {
                        val duration = event.duration.coerceAtLeast(1)
                        val tps = if (event.tokenCount > 0) event.tokenCount * 1000f / duration else 0f
                        send(
                            InferenceChunk(
                                token = "",
                                isThinking = false,
                                isDone = true,
                                tokensGenerated = event.tokenCount,
                                tokensPerSecond = tps,
                                generationTimeMs = duration
                            )
                        )
                        finished.complete(Unit)
                        close()
                    }
                    is LlamaHelper.LLMEvent.Error -> {
                        _lastError.value = "Generation failed: ${event.message}"
                        finished.complete(Unit)
                        close(RuntimeException(event.message))
                    }
                    else -> Unit
                }
            }
        }

        try {
            // Queues the native generation; tokens arrive via engineFlow (collector above).
            helper.predict(prompt)
            // Hold the channel open until the native run signals Done/Error, so the
            // UI keeps streaming until the model is actually finished.
            finished.await()
        } catch (ce: kotlinx.coroutines.CancellationException) {
            throw ce // never swallow structured-cancellation (stop button)
        } catch (e: Exception) {
            close(RuntimeException(e.message ?: "Native generation failed", e))
        } finally {
            collector.cancel()
            // If the caller cancelled this flow (stop button), halt native generation too.
            if (!finished.isCompleted) {
                try {
                    helper.stopPrediction()
                } catch (e: Exception) {
                    Log.w(TAG, "stopPrediction during close failed", e)
                }
            }
        }
    }

    /** Stops the current generation mid-stream (keeps the model loaded in RAM). */
    fun stopGeneration() {
        try {
            helper.stopPrediction()
        } catch (e: Exception) {
            Log.w(TAG, "stopPrediction failed", e)
        }
    }

    /** Frees the native context + weights from RAM. */
    fun release() {
        stopGeneration()
        try {
            helper.release()
        } catch (e: Exception) {
            Log.w(TAG, "release failed", e)
        }
        loadedRef.set(null)
        _loadedModel.value = null
    }

    /** Tears down the engine completely (call from ViewModel.onCleared). */
    fun shutdown() {
        release()
        scope.cancel()
    }

    internal enum class ThinkMarker { OPEN, CLOSE, NONE }

    companion object {
        private const val TAG = "LlamaCppEngine"
        private const val LOAD_TIMEOUT_MS = 120_000L

        /**
         * Detects the reasoning delimiters as emitted by real DeepSeek GGUF tokenizers.
         *
         * Those markers are famously exotic: they carry a zero-width space and full-width
         * bars, and can also arrive split across tokens. So instead of an exact literal
         * match we look for a short token that mentions "think" and closes with '>' —
         * which is unambiguous in practice for a tokenizer's special token.
         */
        internal fun classifyThinkMarker(token: String): ThinkMarker {
            val lower = token.lowercase()
            if (!lower.contains("think")) return ThinkMarker.NONE
            if (!lower.trimEnd().endsWith(">")) return ThinkMarker.NONE
            if (token.length > MAX_MARKER_LENGTH) return ThinkMarker.NONE
            return if (lower.contains('/')) ThinkMarker.CLOSE else ThinkMarker.OPEN
        }

        /** Removes the delimiter text while keeping any real content glued to it. */
        internal fun stripThinkMarker(token: String): String? {
            val stripped = token
                .replace("\u200B", "")
                .replace(Regex("</\\s*think\\s*>"), "")
                .replace(Regex("<\\s*think\\s*>"), "")
            return stripped.ifEmpty { null }
        }

        private const val MAX_MARKER_LENGTH = 24
    }
}
