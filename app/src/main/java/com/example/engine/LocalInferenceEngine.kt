package com.example.engine

import com.example.model.ModelItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.Locale
import kotlin.math.pow

data class InferenceChunk(
    val token: String,
    val isThinking: Boolean = false,
    val isDone: Boolean = false,
    val tokensGenerated: Int = 0,
    val tokensPerSecond: Float = 0f,
    val generationTimeMs: Long = 0L,
    val memoryUsageMb: Int = 0
)

/**
 * Chat-generation entry point.
 *
 * Routes to the real on-device llama.cpp engine ([LlamaCppEngine]) when the model's
 * GGUF file is available — loading it into RAM automatically on first use. There is
 * deliberately NO canned/fake response path and NO cloud fallback: PocketLLM is an
 * offline-first app, so if no model is downloaded yet the user gets clear guidance
 * instead of a hallucinated answer.
 */
class LocalInferenceEngine(private val llamaEngine: LlamaCppEngine? = null) {

    fun generateStream(
        model: ModelItem,
        modelFile: File?,
        messages: List<Pair<String, String>>,
        systemPrompt: String,
        temperature: Float = 0.7f,
        topP: Float = 0.9f
    ): Flow<InferenceChunk> = flow {
        val localFile = modelFile ?: model.localFilePath?.let { File(it) }
        val engine = llamaEngine

        // Real on-device inference via llama.cpp — the only genuine generation path.
        if (engine != null && localFile != null && localFile.exists() && localFile.length() >= 1024) {
            if (!engine.isModelLoaded(model.id)) {
                emit(
                    InferenceChunk(
                        token = "⏳ Loading ${model.name} into RAM (one-time, depends on model size)…\n\n",
                        isThinking = true
                    )
                )
                val loaded = engine.loadIntoRam(model)
                if (loaded == null) {
                    emit(
                        InferenceChunk(
                            token = "❌ Couldn't load **${model.name}** on this device — it may need more free RAM " +
                                "than what's available right now. Try a smaller model (e.g. SmolLM2 135M, ~95 MB) " +
                                "or close other apps and retry.",
                            isDone = true
                        )
                    )
                    return@flow
                }
            }
            engine.generateStream(
                history = messages,
                systemPrompt = systemPrompt,
                temperature = temperature,
                topP = topP
            ).collect { emit(it) }
            return@flow
        }

        // No local GGUF: tell the user exactly what to do instead of faking a reply.
        val guidance = buildString {
            append("📥 **${model.name} is not downloaded yet.**\n\n")
            append("PocketLLM runs models 100% offline on this device — there is no cloud fallback, ")
            append("so nothing is sent over the network and no API key is needed.\n\n")
            append("To chat with this model:\n")
            append("1. Open the **Models** tab\n")
            append("2. Tap **Download** on the model card (Wi-Fi recommended)\n")
            append("3. Come back here and send your message again\n\n")
            append("💡 SmolLM2 135M (~95 MB) runs on any phone and is a great first download. ")
            append("Prefer bigger models without using device RAM? Connect an **Ollama** server in the Ollama tab.")
        }
        emit(InferenceChunk(token = guidance, isDone = true))
    }.flowOn(Dispatchers.IO)

    /**
     * Minimal arithmetic helper (e.g. "what is 2+2", "calculate 15 * 8").
     * Kept as a tiny offline nicety for unit tests and quick mental-math prompts.
     */
    internal fun solveMathExpression(input: String): Pair<String, String>? {
        val clean = input.replace("?", "").replace("can you say", "", ignoreCase = true)
            .replace("what is", "", ignoreCase = true)
            .replace("calculate", "", ignoreCase = true)
            .replace("how much is", "", ignoreCase = true)
            .replace("tell me", "", ignoreCase = true)
            .replace("equals", "=", ignoreCase = true)
            .trim()

        val basicOpRegex = "([0-9]+(?:\\.[0-9]+)?)\\s*([+\\-*/xX×÷^])\\s*([0-9]+(?:\\.[0-9]+)?)".toRegex()
        val match = basicOpRegex.find(clean)
        if (match != null) {
            val a = match.groupValues[1].toDoubleOrNull() ?: return null
            val op = match.groupValues[2]
            val b = match.groupValues[3].toDoubleOrNull() ?: return null

            val result = when (op) {
                "+" -> a + b
                "-" -> a - b
                "*", "x", "X", "×" -> a * b
                "/", "÷" -> if (b != 0.0) a / b else Double.NaN
                "^" -> a.pow(b)
                else -> return null
            }

            val formattedA = if (a % 1 == 0.0) a.toLong().toString() else a.toString()
            val formattedB = if (b % 1 == 0.0) b.toLong().toString() else b.toString()
            val formattedRes = if (result.isNaN()) "undefined (division by zero)" else if (result % 1 == 0.0) result.toLong().toString() else String.format(Locale.US, "%.4f", result).trimEnd('0').trimEnd('.')

            val humanOp = when (op) {
                "*", "x", "X" -> "×"
                "/" -> "÷"
                else -> op
            }

            return Pair("$formattedA $humanOp $formattedB", "$formattedA $humanOp $formattedB = **$formattedRes**")
        }

        return null
    }
}
