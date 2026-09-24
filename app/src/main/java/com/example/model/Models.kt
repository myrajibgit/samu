package com.example.model

enum class DownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

enum class MessageSender {
    USER,
    ASSISTANT,
    SYSTEM
}

data class ModelItem(
    val id: String,
    val name: String,
    val family: String,
    val parameterCount: String,
    val quantization: String,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val downloadUrl: String,
    val localFilePath: String? = null,
    val downloadStatus: DownloadStatus = DownloadStatus.NOT_DOWNLOADED,
    val downloadProgress: Float = 0f,
    val downloadSpeed: String = "",
    val downloadedBytes: Long = 0L,
    val isLoadedInRam: Boolean = false,
    val contextLength: Int = 2048,
    val description: String = "",
    val recommendedRam: String = "2 GB",
    val tags: List<String> = emptyList(),
    val isCustomImport: Boolean = false,
    val isOllamaRemote: Boolean = false
)

data class ChatMessage(
    val id: Long = 0,
    val conversationId: Long,
    val sender: MessageSender,
    val content: String,
    val thinkingContent: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val tokensPerSecond: Float? = null,
    val totalTokens: Int? = null,
    val generationTimeMs: Long? = null,
    val modelUsed: String? = null,
    val isStreaming: Boolean = false
)

data class Conversation(
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val modelId: String = "llama-3.2-1b",
    val systemPrompt: String = "You are a helpful, concise AI assistant running locally on the user's mobile device.",
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f
)

data class GgufMetadata(
    val isValidGguf: Boolean,
    val version: Int = 0,
    val tensorCount: Long = 0L,
    val metadataKvCount: Long = 0L,
    val architecture: String = "Unknown",
    val modelName: String = "Unknown",
    val contextLength: Long = 0L,
    val embeddingLength: Long = 0L,
    val blockCount: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val rawProperties: Map<String, String> = emptyMap(),
    val errorMessage: String? = null
)

data class DeviceHardwareStats(
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val freeStorageBytes: Long,
    val totalStorageBytes: Long,
    val cpuCores: Int,
    val deviceModel: String
) {
    val ramUsedPercent: Float
        get() = if (totalRamBytes > 0) ((totalRamBytes - availableRamBytes).toFloat() / totalRamBytes.toFloat()) else 0f

    val availableRamGb: Float
        get() = availableRamBytes / (1024f * 1024f * 1024f)

    val totalRamGb: Float
        get() = totalRamBytes / (1024f * 1024f * 1024f)

    val freeStorageGb: Float
        get() = freeStorageBytes / (1024f * 1024f * 1024f)
}
