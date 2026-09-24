package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ChatMessageDao
import com.example.data.local.dao.ConversationDao
import com.example.data.local.dao.ModelDao
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.ModelEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ModelEntity::class,
        ConversationEntity::class,
        ChatMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PocketLlmDatabase : RoomDatabase() {

    abstract fun modelDao(): ModelDao
    abstract fun conversationDao(): ConversationDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: PocketLlmDatabase? = null

        fun getDatabase(context: Context): PocketLlmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PocketLlmDatabase::class.java,
                    "pocket_llm_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(database: PocketLlmDatabase) {
                // Populate default curated models
                val defaultModels = CuratedModels.getDefaultModels()
                database.modelDao().insertAll(defaultModels)

                // Populate initial welcome conversation
                val convId = database.conversationDao().insert(
                    ConversationEntity(
                        title = "Welcome to PocketLLM",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        modelId = "smollm2-135m",
                        systemPrompt = "You are PocketLLM, an intelligent, private local AI assistant running on mobile devices. You provide clear, concise and helpful answers.",
                        temperature = 0.7f,
                        topP = 0.9f
                    )
                )

                database.chatMessageDao().insert(
                    ChatMessageEntity(
                        conversationId = convId,
                        sender = "ASSISTANT",
                        content = "👋 Welcome to **PocketLLM**!\n\nI'm your private on-device AI assistant. Here is what you can do:\n\n1. **1-Tap Model Downloads**: Head to the **Models** tab to download lightweight GGUF models directly to your phone (like SmolLM2, Llama 3.2 1B, Qwen 2.5, DeepSeek-R1).\n2. **Ollama Hub Bridge**: Connect to your PC/Mac or local server running Ollama (`localhost:11434` or network IP) to run any model without downloading.\n3. **GGUF Inspector**: View tensor counts, quantization format, and context length for imported weights.\n4. **100% Private**: Your conversations and local models stay entirely on your device.\n\nTry sending a message or tap the model chip above to inspect or switch models!",
                        timestamp = System.currentTimeMillis() - 1000,
                        tokensPerSecond = 34.2f,
                        totalTokens = 128,
                        generationTimeMs = 3740,
                        modelUsed = "SmolLM2-135M (Local)"
                    )
                )
            }
        }
    }
}
