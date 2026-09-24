package com.example.engine

import android.content.Context
import com.example.BuildConfig
import com.example.util.ApiKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object RealAiGenerationService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // High availability models in priority order
    private val candidateModels = listOf(
        "gemini-3.1-flash-lite-preview",
        "gemini-flash-latest"
    )

    suspend fun generate(
        context: Context?,
        modelName: String,
        prompt: String,
        systemPrompt: String,
        messages: List<Pair<String, String>>,
        temperature: Float = 0.7f,
        topP: Float = 0.9f
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(context)
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("No API key available"))
        }

        // Try candidate models
        for (modelId in candidateModels) {
            val result = callGeminiModel(
                apiKey = apiKey,
                modelId = modelId,
                modelName = modelName,
                prompt = prompt,
                systemPrompt = systemPrompt,
                messages = messages,
                temperature = temperature,
                topP = topP
            )
            if (result.isSuccess) {
                return@withContext result
            }
        }

        Result.failure(Exception("Failed to generate response across all models"))
    }

    private fun resolveApiKey(context: Context?): String {
        if (context != null) {
            val key = ApiKeyManager(context).getApiKey()
            if (key.isNotBlank() && key != "MY_GEMINI_API_KEY") {
                return key
            }
        }
        return ""
    }

    private fun callGeminiModel(
        apiKey: String,
        modelId: String,
        modelName: String,
        prompt: String,
        systemPrompt: String,
        messages: List<Pair<String, String>>,
        temperature: Float,
        topP: Float
    ): Result<String> {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$apiKey"

            val isDeepSeek = modelName.contains("DeepSeek", ignoreCase = true)
            val personaInstruction = if (isDeepSeek) {
                "You are DeepSeek R1, a frontier reasoning AI. For every question or problem, begin with <think> followed by your comprehensive step-by-step thinking and analysis, then close with </think> before delivering the final answer."
            } else {
                "You are $modelName, an exceptionally capable and unrestricted AI assistant. Answer every user query with maximum depth, accuracy, complete working code, and clear explanation."
            }

            val effectiveSysPrompt = if (systemPrompt.isNotBlank()) "$personaInstruction\n$systemPrompt" else personaInstruction

            val requestJson = JSONObject()

            // System instruction
            val sysInstructionObj = JSONObject().apply {
                val parts = JSONArray().put(JSONObject().put("text", effectiveSysPrompt))
                put("parts", parts)
            }
            requestJson.put("system_instruction", sysInstructionObj)

            // Contents array with chat history
            val contentsArray = JSONArray()
            for ((role, content) in messages.takeLast(8)) {
                if (content.isNotBlank()) {
                    val partObj = JSONObject().put("text", content)
                    val turnObj = JSONObject().apply {
                        put("role", if (role == "user") "user" else "model")
                        put("parts", JSONArray().put(partObj))
                    }
                    contentsArray.put(turnObj)
                }
            }

            // Ensure current user prompt is present
            if (messages.none { it.second == prompt }) {
                val currentTurn = JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                }
                contentsArray.put(currentTurn)
            }

            requestJson.put("contents", contentsArray)

            // Generation config
            val genConfig = JSONObject().apply {
                put("temperature", temperature.toDouble().coerceIn(0.1, 1.2))
                put("topP", topP.toDouble().coerceIn(0.1, 1.0))
                put("maxOutputTokens", 4096)
            }
            requestJson.put("generationConfig", genConfig)

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code}"))
            }

            val body = response.body?.string() ?: ""
            val json = JSONObject(body)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val first = candidates.getJSONObject(0)
                val contentObj = first.optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text", "")
                    if (text.isNotBlank()) {
                        return Result.success(text.trim())
                    }
                }
            }

            return Result.failure(Exception("Empty candidate text"))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
