package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.ModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {
    @Query("SELECT * FROM models ORDER BY family ASC, sizeBytes ASC")
    fun getAllModelsFlow(): Flow<List<ModelEntity>>

    @Query("SELECT * FROM models WHERE id = :id LIMIT 1")
    suspend fun getModelById(id: String): ModelEntity?

    @Query("SELECT * FROM models WHERE downloadStatus = 'COMPLETED'")
    fun getInstalledModelsFlow(): Flow<List<ModelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(model: ModelEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(models: List<ModelEntity>)

    @Update
    suspend fun update(model: ModelEntity)

    @Query("UPDATE models SET downloadStatus = :status, downloadProgress = :progress, downloadSpeed = :speed, downloadedBytes = :downloadedBytes, localFilePath = :localPath WHERE id = :id")
    suspend fun updateDownloadProgress(
        id: String,
        status: String,
        progress: Float,
        speed: String,
        downloadedBytes: Long,
        localPath: String?
    )

    @Query("UPDATE models SET isLoadedInRam = :loaded WHERE id = :id")
    suspend fun updateLoadedInRam(id: String, loaded: Boolean)

    @Query("UPDATE models SET isLoadedInRam = 0")
    suspend fun unloadAllModels()

    @Delete
    suspend fun delete(model: ModelEntity)

    @Query("DELETE FROM models WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversationsFlow(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: Long): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: ConversationEntity): Long

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Query("UPDATE conversations SET updatedAt = :timestamp WHERE id = :id")
    suspend fun touch(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET modelId = :modelId WHERE id = :id")
    suspend fun updateModel(id: Long, modelId: String)

    @Query("UPDATE conversations SET systemPrompt = :systemPrompt, temperature = :temperature, topP = :topP WHERE id = :id")
    suspend fun updateSettings(id: Long, systemPrompt: String, temperature: Float, topP: Float)

    @Delete
    suspend fun delete(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversationFlow(conversationId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesForConversation(conversationId: Long): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity): Long

    @Update
    suspend fun update(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteByConversationId(conversationId: Long)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteById(id: Long)
}
