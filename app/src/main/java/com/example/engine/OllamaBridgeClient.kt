package com.example.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class OllamaModel(
    val name: String,
    val sizeBytes: Long,
    val format: String,
    val family: String,
    val parameterSize: String,
    val quantizationLevel: String
)

data class OllamaStreamChunk(
    val text: String,
    val isDone: Boolean,
    val tokensGenerated: Int = 0,
    val evalDurationNanos: Long = 0L
) {
    val tokensPerSecond: Float
        get() = if (evalDurationNanos > 0) {
            (tokensGenerated.toFloat() / (evalDurationNanos.toFloat() / 1_000_000_000f))
        } else 0f
}

data class OllamaPullProgress(
    val status: String,
    val completedBytes: Long = 0L,
    val totalBytes: Long = 0L
) {
    val progress: Float
        get() = if (totalBytes > 0) (completedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

class OllamaBridgeClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun checkConnection(baseUrl: String): Result<List<OllamaModel>> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val sanitizedUrl = sanitizeBaseUrl(baseUrl)
            val request = Request.Builder()
                .url("$sanitizedUrl/api/tags")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
                val body = response.body?.string() ?: "{}"
                val json = JSONObject(body)
                val modelsArray = json.optJSONArray("models") ?: JSONArray()
                val list = mutableListOf<OllamaModel>()

                for (i in 0 until modelsArray.length()) {
                    val m = modelsArray.getJSONObject(i)
                    val details = m.optJSONObject("details") ?: JSONObject()
                    list.add(
                        OllamaModel(
                            name = m.optString("name", "Unknown"),
                            sizeBytes = m.optLong("size", 0L),
                            format = details.optString("format", "gguf"),
                            family = details.optString("family", "llama"),
                            parameterSize = details.optString("parameter_size", "N/A"),
                            quantizationLevel = details.optString("quantization_level", "Q4_K_M")
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun chatStream(
        baseUrl: String,
        modelName: String,
        messages: List<Pair<String, String>>, // role, content
        systemPrompt: String = "",
        temperature: Float = 0.7f,
        topP: Float = 0.9f
    ): Flow<OllamaStreamChunk> = flow {
        val sanitizedUrl = sanitizeBaseUrl(baseUrl)

        val messagesJson = JSONArray()
        if (systemPrompt.isNotBlank()) {
            val sysObj = JSONObject()
            sysObj.put("role", "system")
            sysObj.put("content", systemPrompt)
            messagesJson.put(sysObj)
        }

        for ((role, content) in messages) {
            val msgObj = JSONObject()
            msgObj.put("role", role)
            msgObj.put("content", content)
            messagesJson.put(msgObj)
        }

        val optionsJson = JSONObject().apply {
            put("temperature", temperature)
            put("top_p", topP)
        }

        val requestBodyJson = JSONObject().apply {
            put("model", modelName)
            put("messages", messagesJson)
            put("stream", true)
            put("options", optionsJson)
        }

        val request = Request.Builder()
            .url("$sanitizedUrl/api/chat")
            .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Ollama error: HTTP ${response.code} ${response.message}")
        }

        val body = response.body ?: throw Exception("Empty response body from Ollama")
        val reader = BufferedReader(InputStreamReader(body.byteStream()))

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val trimmed = line?.trim() ?: continue
            if (trimmed.isEmpty()) continue

            try {
                val json = JSONObject(trimmed)
                val msg = json.optJSONObject("message")
                val text = msg?.optString("content") ?: ""
                val done = json.optBoolean("done", false)
                val evalCount = json.optInt("eval_count", 0)
                val evalDuration = json.optLong("eval_duration", 0L)

                emit(
                    OllamaStreamChunk(
                        text = text,
                        isDone = done,
                        tokensGenerated = evalCount,
                        evalDurationNanos = evalDuration
                    )
                )

                if (done) break
            } catch (e: Exception) {
                // Ignore parse errors on trailing lines
            }
        }
    }.flowOn(Dispatchers.IO)

    fun pullModel(
        baseUrl: String,
        modelName: String
    ): Flow<OllamaPullProgress> = flow {
        val sanitizedUrl = sanitizeBaseUrl(baseUrl)
        val bodyJson = JSONObject().apply {
            put("name", modelName)
            put("stream", true)
        }

        val request = Request.Builder()
            .url("$sanitizedUrl/api/pull")
            .post(bodyJson.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Pull error: HTTP ${response.code} ${response.message}")
        }

        val body = response.body ?: throw Exception("Empty pull response")
        val reader = BufferedReader(InputStreamReader(body.byteStream()))

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val trimmed = line?.trim() ?: continue
            if (trimmed.isEmpty()) continue
            try {
                val json = JSONObject(trimmed)
                val status = json.optString("status", "Pulling...")
                val completed = json.optLong("completed", 0L)
                val total = json.optLong("total", 0L)
                emit(OllamaPullProgress(status = status, completedBytes = completed, totalBytes = total))
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    private fun sanitizeBaseUrl(url: String): String {
        var clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "http://$clean"
        }
        if (clean.endsWith("/")) {
            clean = clean.dropLast(1)
        }
        return clean
    }
}
