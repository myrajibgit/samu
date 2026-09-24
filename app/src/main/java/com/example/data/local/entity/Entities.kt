package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "models")
data class ModelEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val family: String,
    val parameterCount: String,
    val quantization: String,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val downloadUrl: String,
    val localFilePath: String? = null,
    val downloadStatus: String = "NOT_DOWNLOADED",
    val downloadProgress: Float = 0f,
    val downloadSpeed: String = "",
    val downloadedBytes: Long = 0L,
    val isLoadedInRam: Boolean = false,
    val contextLength: Int = 2048,
    val description: String = "",
    val recommendedRam: String = "2 GB",
    val tags: String = "",
    val isCustomImport: Boolean = false
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val modelId: String = "llama-3.2-1b",
    val systemPrompt: String = "You are a helpful, concise AI assistant running locally on the user's mobile device.",
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val sender: String,
    val content: String,
    val thinkingContent: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val tokensPerSecond: Float? = null,
    val totalTokens: Int? = null,
    val generationTimeMs: Long? = null,
    val modelUsed: String? = null
)
