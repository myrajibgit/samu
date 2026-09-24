package com.example.engine

import android.content.Context
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

object GeminiCloudService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun isConfigured(context: Context): Boolean {
        val apiKey = ApiKeyManager(context).getApiKey()
        return apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"
    }

    suspend fun generateResponse(
        context: Context,
        modelName: String,
        prompt: String,
        systemPrompt: String,
        messages: List<Pair<String, String>>,
        temperature: Float = 0.7f,
        topP: Float = 0.9f
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = ApiKeyManager(context).getApiKey()
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(Exception("No API key configured"))
            }

            // Using modern supported Gemini 3.5 Flash model
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val contentsArray = JSONArray()

            // System instructions tailoring persona
            val isDeepSeek = modelName.contains("DeepSeek", ignoreCase = true)
            val personaInstruction = if (isDeepSeek) {
                "You are $modelName. When solving complex or analytical problems, you MUST enclose your internal thinking process inside <think> and </think> tags before giving the final answer."
            } else {
                "You are $modelName, a high-intelligence open-weights model running for the user. Answer all questions with deep, accurate knowledge, complete code snippets, and clear explanations."
            }

            val fullSystemPrompt = if (systemPrompt.isNotBlank()) {
                "$personaInstruction\nUser Guidelines: $systemPrompt"
            } else personaInstruction

            // Build dialog turns
            val historyBuilder = StringBuilder("System Instructions: $fullSystemPrompt\n\n")
            for ((role, content) in messages.takeLast(6)) {
                val label = if (role == "user") "User" else "Assistant"
                historyBuilder.append("$label: $content\n")
            }
            if (!historyBuilder.contains("User: $prompt")) {
                historyBuilder.append("User: $prompt\n")
            }
            historyBuilder.append("Assistant: ")

            val userObj = JSONObject()
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", historyBuilder.toString()))
            userObj.put("role", "user")
            userObj.put("parts", partsArray)
            contentsArray.put(userObj)

            val requestJson = JSONObject().apply {
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", temperature.toDouble().coerceIn(0.1, 1.5))
                    put("topP", topP.toDouble().coerceIn(0.1, 1.0))
                    put("maxOutputTokens", 2048)
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                return@withContext Result.failure(Exception("HTTP ${response.code}: $errBody"))
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
                        return@withContext Result.success(text.trim())
                    }
                }
            }

            Result.failure(Exception("No candidate text returned"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
