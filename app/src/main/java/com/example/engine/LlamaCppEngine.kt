package com.example.engine

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.example.model.ModelItem
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
import kotlinx.coroutines.flow.awaitClose
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

    fun isModelLoaded(modelId: String): Boolean = loadedRef.get()?.modelId == modelId

    /**
     * Loads a GGUF file into RAM. Loading another model releases the previous one.
     * Suspend + bounded by [LOAD_TIMEOUT_MS]. Returns null on failure.
     */
    suspend fun loadIntoRam(model: ModelItem): LoadedModel? = withContext(Dispatchers.IO) {
        val file = model.localFilePath?.let { File(it) }
        if (file == null || !file.exists() || file.length() < 1024) {
            Log.e(TAG, "Model file missing or too small: ${model.localFilePath}")
            return@withContext null
        }
        loadedRef.get()?.let { if (it.modelId == model.id) return@withContext it }

        _isLoading.value = true
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
                release()
                return@withContext null
            }

            val loaded = result.get()
            if (loaded == null) {
                Log.e(TAG, "Model load timed out after ${LOAD_TIMEOUT_MS / 1000}s")
                release()
                return@withContext null
            }

            loadedRef.set(loaded)
            _loadedModel.value = loaded
            Log.i(TAG, "Model loaded into RAM: ${loaded.name} (ctx=$ctxLen)")
            loaded
        } catch (e: Exception) {
            Log.e(TAG, "Exception during model load", e)
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

        // DeepSeek R1 distills emit <think> reasoning blocks; key off the model id
        // so the UI can route them into the collapsible "thinking" section.
        val isReasoningModel = loaded.modelId.contains("deepseek", ignoreCase = true)
        val templateFamily = if (isReasoningModel) "deepseek" else loaded.family
        val prompt = PromptFormatter.format(templateFamily, history, systemPrompt)

        val startMs = System.currentTimeMillis()

        val collector = launch {
            engineFlow.collect { event ->
                when (event) {
                    is LlamaHelper.LLMEvent.Ongoing -> {
                        val token = event.word
                        val isThinkMarker = isReasoningModel &&
                            (token.contains("<think>") || token.contains("</think>"))
                        val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(1)
                        val tps = if (event.tokenCount > 0) event.tokenCount * 1000f / elapsed else 0f
                        send(
                            InferenceChunk(
                                token = token,
                                isThinking = isThinkMarker,
                                isDone = false,
                                tokensGenerated = event.tokenCount,
                                tokensPerSecond = tps,
                                generationTimeMs = elapsed
                            )
                        )
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
                        close()
                    }
                    is LlamaHelper.LLMEvent.Error ->
                        close(RuntimeException(event.message))
                    else -> Unit
                }
            }
        }

        try {
            // Returns immediately; tokens arrive via engineFlow (see collector above).
            helper.predict(prompt)
        } catch (e: Exception) {
            close(RuntimeException(e.message ?: "Native generation failed", e))
        }

        // Keep the channel open until the native Done event closes it, or the caller
        // cancels this flow (stop button) — then halt the native generation too.
        awaitClose {
            try {
                helper.stopPrediction()
            } catch (e: Exception) {
                Log.w(TAG, "stopPrediction during close failed", e)
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

    companion object {
        private const val TAG = "LlamaCppEngine"
        private const val LOAD_TIMEOUT_MS = 120_000L
    }
}
