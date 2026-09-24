package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

class ApiKeyManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pocket_llm_prefs", Context.MODE_PRIVATE)

    fun getApiKey(): String {
        return prefs.getString("user_gemini_api_key", "")?.trim() ?: ""
    }

    fun saveApiKey(key: String) {
        prefs.edit().putString("user_gemini_api_key", key.trim()).apply()
    }

    fun clearApiKey() {
        prefs.edit().remove("user_gemini_api_key").apply()
    }

    fun isConfigured(): Boolean {
        return getApiKey().isNotBlank()
    }
}
