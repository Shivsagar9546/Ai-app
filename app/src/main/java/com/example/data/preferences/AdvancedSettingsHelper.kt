package com.example.data.preferences

import android.content.Context

class AdvancedSettingsHelper(private val context: Context) {
    private val prefs = context.getSharedPreferences("admin_advanced_settings", Context.MODE_PRIVATE)

    var geminiApiKey2: String
        get() = prefs.getString("gemini_api_key_2", "") ?: ""
        set(value) = prefs.edit().putString("gemini_api_key_2", value).apply()

    var geminiApiKey3: String
        get() = prefs.getString("gemini_api_key_3", "") ?: ""
        set(value) = prefs.edit().putString("gemini_api_key_3", value).apply()

    var geminiApiKey4: String
        get() = prefs.getString("gemini_api_key_4", "") ?: ""
        set(value) = prefs.edit().putString("gemini_api_key_4", value).apply()

    var geminiApiKey5: String
        get() = prefs.getString("gemini_api_key_5", "") ?: ""
        set(value) = prefs.edit().putString("gemini_api_key_5", value).apply()

    var dailyQuotaLimit: Int
        get() = prefs.getInt("daily_quota_limit", 0) // 0 means Unlimited (no quota or hit limit)
        set(value) = prefs.edit().putInt("daily_quota_limit", value).apply()

    var isMaintenanceMode: Boolean
        get() = prefs.getBoolean("is_maintenance_mode", false)
        set(value) = prefs.edit().putBoolean("is_maintenance_mode", value).apply()

    var adminSecurityQuestion: String
        get() = prefs.getString("admin_security_question", "") ?: ""
        set(value) = prefs.edit().putString("admin_security_question", value).apply()

    var adminSecurityAnswer: String
        get() = prefs.getString("admin_security_answer", "") ?: ""
        set(value) = prefs.edit().putString("admin_security_answer", value).apply()

    var isAutoSpeakEnabled: Boolean
        get() = prefs.getBoolean("is_auto_speak_enabled", false)
        set(value) = prefs.edit().putBoolean("is_auto_speak_enabled", value).apply()

    var isClipboardListenerEnabled: Boolean
        get() = prefs.getBoolean("is_clipboard_listener_enabled", false)
        set(value) = prefs.edit().putBoolean("is_clipboard_listener_enabled", value).apply()
        
    // Track API performance speed
    var lastApiLatencyMs: Long
        get() = prefs.getLong("last_api_latency_ms", 0)
        set(value) = prefs.edit().putLong("last_api_latency_ms", value).apply()

    // Local user feedbacks list (stored as simple string lines for ease)
    fun addFeedback(text: String) {
        val current = prefs.getString("user_feedbacks", "") ?: ""
        val newFeedback = "[${System.currentTimeMillis()}] $text"
        val updated = if (current.isEmpty()) newFeedback else "$current\n$newFeedback"
        prefs.edit().putString("user_feedbacks", updated).apply()
    }

    fun getFeedbacks(): List<String> {
        val current = prefs.getString("user_feedbacks", "") ?: ""
        if (current.isEmpty()) return emptyList()
        return current.split("\n")
    }

    fun clearFeedbacks() {
        prefs.edit().remove("user_feedbacks").apply()
    }
}
