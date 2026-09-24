package com.example.data.repository

import com.example.data.local.dao.ChatMessageDao
import com.example.data.local.dao.ConversationDao
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ConversationEntity
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.MessageSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ChatRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: ChatMessageDao
) {
    fun getAllConversationsFlow(): Flow<List<Conversation>> {
        return conversationDao.getAllConversationsFlow().map { entities ->
            entities.map { it.toConversation() }
        }
    }

    fun getMessagesFlow(conversationId: Long): Flow<List<ChatMessage>> {
        return messageDao.getMessagesForConversationFlow(conversationId).map { entities ->
            entities.map { it.toChatMessage() }
        }
    }

    suspend fun getConversationById(id: Long): Conversation? = withContext(Dispatchers.IO) {
        conversationDao.getConversationById(id)?.toConversation()
    }

    suspend fun createConversation(
        title: String = "New Chat",
        modelId: String = "llama-3.2-1b",
        systemPrompt: String = "You are a helpful, concise AI assistant running locally on mobile devices."
    ): Long = withContext(Dispatchers.IO) {
        val entity = ConversationEntity(
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            modelId = modelId,
            systemPrompt = systemPrompt,
            temperature = 0.7f,
            topP = 0.9f
        )
        conversationDao.insert(entity)
    }

    suspend fun updateConversation(conversation: Conversation) = withContext(Dispatchers.IO) {
        conversationDao.update(
            ConversationEntity(
                id = conversation.id,
                title = conversation.title,
                createdAt = conversation.createdAt,
                updatedAt = System.currentTimeMillis(),
                modelId = conversation.modelId,
                systemPrompt = conversation.systemPrompt,
                temperature = conversation.temperature,
                topP = conversation.topP
            )
        )
    }

    suspend fun deleteConversation(conversationId: Long) = withContext(Dispatchers.IO) {
        messageDao.deleteByConversationId(conversationId)
        conversationDao.deleteById(conversationId)
    }

    suspend fun saveMessage(message: ChatMessage): Long = withContext(Dispatchers.IO) {
        val id = messageDao.insert(
            ChatMessageEntity(
                id = message.id,
                conversationId = message.conversationId,
                sender = message.sender.name,
                content = message.content,
                thinkingContent = message.thinkingContent,
                timestamp = message.timestamp,
                tokensPerSecond = message.tokensPerSecond,
                totalTokens = message.totalTokens,
                generationTimeMs = message.generationTimeMs,
                modelUsed = message.modelUsed
            )
        )
        conversationDao.touch(message.conversationId)
        id
    }

    suspend fun updateMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        messageDao.update(
            ChatMessageEntity(
                id = message.id,
                conversationId = message.conversationId,
                sender = message.sender.name,
                content = message.content,
                thinkingContent = message.thinkingContent,
                timestamp = message.timestamp,
                tokensPerSecond = message.tokensPerSecond,
                totalTokens = message.totalTokens,
                generationTimeMs = message.generationTimeMs,
                modelUsed = message.modelUsed
            )
        )
    }

    private fun ConversationEntity.toConversation(): Conversation {
        return Conversation(
            id = id,
            title = title,
            createdAt = createdAt,
            updatedAt = updatedAt,
            modelId = modelId,
            systemPrompt = systemPrompt,
            temperature = temperature,
            topP = topP
        )
    }

    private fun ChatMessageEntity.toChatMessage(): ChatMessage {
        val msgSender = try {
            MessageSender.valueOf(sender)
        } catch (_: Exception) {
            MessageSender.USER
        }
        return ChatMessage(
            id = id,
            conversationId = conversationId,
            sender = msgSender,
            content = content,
            thinkingContent = thinkingContent,
            timestamp = timestamp,
            tokensPerSecond = tokensPerSecond,
            totalTokens = totalTokens,
            generationTimeMs = generationTimeMs,
            modelUsed = modelUsed,
            isStreaming = false
        )
    }
}
