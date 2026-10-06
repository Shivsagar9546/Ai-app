package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "admin_settings")

data class AdminSettings(
    val defaultProvider: String = "gemini", // "gemini", "openai", "poe", or "openrouter"
    val geminiApiKey: String = "",
    val geminiModel: String = "auto",
    val isGeminiEnabled: Boolean = true,
    val openAiApiKey: String = "",
    val openAiModel: String = "gpt-4o-mini",
    val isOpenAiEnabled: Boolean = true,
    val poeApiKey: String = "",
    val poeModel: String = "",
    val isPoeEnabled: Boolean = true,
    val poeModelsJson: String = "",
    val openRouterApiKey: String = "",
    val openRouterModel: String = "google/gemini-2.5-flash:free",
    val isOpenRouterEnabled: Boolean = true,
    val openRouterModelsJson: String = "",
    val isFallbackEnabled: Boolean = true,
    val isWebSearchEnabled: Boolean = false,
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val isScreenScanEnabled: Boolean = true,
    val isAreaScanEnabled: Boolean = true,
    val maxImageResolution: Int = 1024,
    val adminPin: String = "1234",
    val appTheme: String = "system", // "system", "dark", "light"
    val preferredLanguage: String = "hinglish", // "en", "hi", "hinglish"
    val totalRequests: Int = 0,
    val todayRequests: Int = 0,
    val geminiRequests: Int = 0,
    val openAiRequests: Int = 0,
    val poeRequests: Int = 0,
    val openRouterRequests: Int = 0,
    val screenScanRequests: Int = 0,
    val errorCount: Int = 0,
    val lastRequestDate: String = "",
    val bubbleStyle: String = "pill", // "pill", "circle", "icon_only", "square"
    val bubbleCustomImagePath: String = "",
    val bubblePresetIcon: String = "sparkle", // "sparkle", "robot", "brain", "flash", "target", "flame", "diamond", "eye"
    val bubbleText: String = "AI ✨",
    val bubbleGradient: String = "purple", // "purple", "cyan", "sunset", "emerald", "dark", "gold"
    val bubbleSize: String = "medium", // "small", "medium", "large"
    val bubbleAlpha: Float = 1.0f
) {
    fun maskKey(key: String): String {
        if (key.isBlank()) return "Not configured"
        if (key.length <= 8) return "••••••••"
        val prefix = key.take(4)
        val suffix = key.takeLast(4)
        return "$prefix••••••••$suffix"
    }
}

val DEFAULT_SYSTEM_PROMPT = """You are OmniAI, a premier AI tutor and academic solver with the crystal-clear, pedagogical mastery and step-by-step elegance of ChatGPT (GPT-4o).

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🎯 MANDATORY ACADEMIC & QUESTION ANSWERING RULES:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

जब भी कोई Physics, Chemistry, Mathematics, Biology या अन्य academic question/image मिले, निम्नलिखित नियमों का कड़ाई से पालन करें:

1️⃣ शुरुआत में सही उत्तर (STARTING FORMAT):
- सबसे पहली लाइन में बिना किसी देरी के सही उत्तर/विकल्प दिखाएं:
  **सही उत्तर है: Option (C) 0.25V** (यदि MCQ है)
  या
  **सही उत्तर है: 0.25V** (यदि numerical/direct प्रश्न है)

2️⃣ केवल जरूरत के अनुसार Step-by-Step Explanation:
- अनावश्यक लंबा भाषण दिए बिना केवल आवश्यक, तार्किक और सटीक Step-by-Step हल दें।
- प्रत्येक Step का स्पष्ट शीर्षक रखें (जैसे: **Step 1: परिपथ का कुल प्रतिरोध निकालना**, **Step 2: मुख्य धारा (Current) की गणना**)।

3️⃣ Physics/Math में सभी Equations Proper LaTeX में दें:
- Inline formulas के लिए '${'$'}equation${'$'}' और Block/Multi-line equations के लिए '${'$'}${'$'}equation${'$'}${'$'}' का उपयोग करें।
- उदाहरण: '${'$'}I = \frac{E}{R_{\text{total}}}${'$'}', '${'$'}${'$'}V = I \times R_{AJ} = 0.5 \times 0.5 = 0.25\text{ V}${'$'}${'$'}'

4️⃣ हर Important Calculation अलग Line में रखें:
- प्रत्येक मुख्य सूत्र और गणना को अलग-अलग पंक्तियों में साफ़-सुथरे तरीके से रखें ताकि पढ़ने में कोई भ्रम न हो।

5️⃣ FORMULA को CODE BLOCK में कभी मत दिखाओ:
- किसी भी गणितीय सूत्र या समीकरण को ``` (code block) में कभी न रखें। हमेशा standard LaTeX math notation ('${'$'}' या '${'$'}${'$'}') का उपयोग करें।

6️⃣ सरल Hindi/Hinglish भाषा का प्रयोग:
- व्याख्या को बहुत सरल, स्वाभाविक Hindi/Hinglish में रखें ताकि कोई भी छात्र इसे पहली बार में ही 100% समझ सके।
- तकनीकी और वैज्ञानिक शब्दों (जैसे: Resistance, Current, EMF, Sphere, Cylinder, Kinetic Energy) को standard English/Hinglish में रखें।

7️⃣ अंत में Final Answer (ENDING FORMAT):
- हल के अंत में निष्कर्ष अवश्य दें:
  **Final Answer: Option (C) 0.25V**

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📚 GENERAL QUERIES & CODING:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- सामान्य प्रश्नों, निबंधों, कोडिंग या अन्य विषयों के लिए भी सुव्यवस्थित Markdown (Bullet points, bold headings, clean formatting) में उत्तर दें।""".trimIndent()

class AdminPreferencesRepository(val context: Context) {

    private object PreferencesKeys {
        val DEFAULT_PROVIDER = stringPreferencesKey("default_provider")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val GEMINI_ENABLED = booleanPreferencesKey("gemini_enabled")
        val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        val OPENAI_MODEL = stringPreferencesKey("openai_model")
        val OPENAI_ENABLED = booleanPreferencesKey("openai_enabled")
        val POE_API_KEY = stringPreferencesKey("poe_api_key")
        val POE_MODEL = stringPreferencesKey("poe_model")
        val POE_ENABLED = booleanPreferencesKey("poe_enabled")
        val POE_MODELS_JSON = stringPreferencesKey("poe_models_json")
        val OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
        val OPENROUTER_MODEL = stringPreferencesKey("openrouter_model")
        val OPENROUTER_ENABLED = booleanPreferencesKey("openrouter_enabled")
        val OPENROUTER_MODELS_JSON = stringPreferencesKey("openrouter_models_json")
        val FALLBACK_ENABLED = booleanPreferencesKey("fallback_enabled")
        val WEB_SEARCH_ENABLED = booleanPreferencesKey("web_search_enabled")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val SCREEN_SCAN_ENABLED = booleanPreferencesKey("screen_scan_enabled")
        val AREA_SCAN_ENABLED = booleanPreferencesKey("area_scan_enabled")
        val MAX_IMAGE_RESOLUTION = intPreferencesKey("max_image_resolution")
        val ADMIN_PIN = stringPreferencesKey("admin_pin")
        val APP_THEME = stringPreferencesKey("app_theme")
        val PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")

        val BUBBLE_STYLE = stringPreferencesKey("bubble_style")
        val BUBBLE_CUSTOM_IMAGE_PATH = stringPreferencesKey("bubble_custom_image_path")
        val BUBBLE_PRESET_ICON = stringPreferencesKey("bubble_preset_icon")
        val BUBBLE_TEXT = stringPreferencesKey("bubble_text")
        val BUBBLE_GRADIENT = stringPreferencesKey("bubble_gradient")
        val BUBBLE_SIZE = stringPreferencesKey("bubble_size")
        val BUBBLE_ALPHA = androidx.datastore.preferences.core.floatPreferencesKey("bubble_alpha")

        val TOTAL_REQUESTS = intPreferencesKey("total_requests")
        val TODAY_REQUESTS = intPreferencesKey("today_requests")
        val GEMINI_REQUESTS = intPreferencesKey("gemini_requests")
        val OPENAI_REQUESTS = intPreferencesKey("openai_requests")
        val POE_REQUESTS = intPreferencesKey("poe_requests")
        val OPENROUTER_REQUESTS = intPreferencesKey("openrouter_requests")
        val SCREEN_SCAN_REQUESTS = intPreferencesKey("screen_scan_requests")
        val ERROR_COUNT = intPreferencesKey("error_count")
        val LAST_REQUEST_DATE = stringPreferencesKey("last_request_date")
    }

    val settingsFlow: Flow<AdminSettings> = context.dataStore.data.map { preferences ->
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val storedDate = preferences[PreferencesKeys.LAST_REQUEST_DATE] ?: today
        val todayCount = if (storedDate == today) {
            preferences[PreferencesKeys.TODAY_REQUESTS] ?: 0
        } else {
            0
        }

        AdminSettings(
            defaultProvider = preferences[PreferencesKeys.DEFAULT_PROVIDER] ?: "gemini",
            geminiApiKey = preferences[PreferencesKeys.GEMINI_API_KEY] ?: "",
            geminiModel = preferences[PreferencesKeys.GEMINI_MODEL] ?: "auto",
            isGeminiEnabled = preferences[PreferencesKeys.GEMINI_ENABLED] ?: true,
            openAiApiKey = preferences[PreferencesKeys.OPENAI_API_KEY] ?: "",
            openAiModel = preferences[PreferencesKeys.OPENAI_MODEL] ?: "gpt-4o-mini",
            isOpenAiEnabled = preferences[PreferencesKeys.OPENAI_ENABLED] ?: true,
            poeApiKey = preferences[PreferencesKeys.POE_API_KEY] ?: "",
            poeModel = preferences[PreferencesKeys.POE_MODEL] ?: "",
            isPoeEnabled = preferences[PreferencesKeys.POE_ENABLED] ?: true,
            poeModelsJson = preferences[PreferencesKeys.POE_MODELS_JSON] ?: "",
            openRouterApiKey = preferences[PreferencesKeys.OPENROUTER_API_KEY] ?: "",
            openRouterModel = preferences[PreferencesKeys.OPENROUTER_MODEL] ?: "google/gemini-flash-1.5:free",
            isOpenRouterEnabled = preferences[PreferencesKeys.OPENROUTER_ENABLED] ?: true,
            openRouterModelsJson = preferences[PreferencesKeys.OPENROUTER_MODELS_JSON] ?: "",
            isFallbackEnabled = preferences[PreferencesKeys.FALLBACK_ENABLED] ?: true,
            isWebSearchEnabled = preferences[PreferencesKeys.WEB_SEARCH_ENABLED] ?: false,
            systemPrompt = preferences[PreferencesKeys.SYSTEM_PROMPT]?.let { stored ->
                if (stored.isBlank() || !stored.contains("MANDATORY ACADEMIC & QUESTION ANSWERING RULES")) {
                    DEFAULT_SYSTEM_PROMPT
                } else {
                    stored
                }
            } ?: DEFAULT_SYSTEM_PROMPT,
            isScreenScanEnabled = preferences[PreferencesKeys.SCREEN_SCAN_ENABLED] ?: true,
            isAreaScanEnabled = preferences[PreferencesKeys.AREA_SCAN_ENABLED] ?: true,
            maxImageResolution = preferences[PreferencesKeys.MAX_IMAGE_RESOLUTION] ?: 1920,
            adminPin = preferences[PreferencesKeys.ADMIN_PIN] ?: "1234",
            appTheme = preferences[PreferencesKeys.APP_THEME] ?: "system",
            preferredLanguage = preferences[PreferencesKeys.PREFERRED_LANGUAGE] ?: "hinglish",
            bubbleStyle = preferences[PreferencesKeys.BUBBLE_STYLE] ?: "pill",
            bubbleCustomImagePath = preferences[PreferencesKeys.BUBBLE_CUSTOM_IMAGE_PATH] ?: "",
            bubblePresetIcon = preferences[PreferencesKeys.BUBBLE_PRESET_ICON] ?: "sparkle",
            bubbleText = preferences[PreferencesKeys.BUBBLE_TEXT] ?: "AI ✨",
            bubbleGradient = preferences[PreferencesKeys.BUBBLE_GRADIENT] ?: "purple",
            bubbleSize = preferences[PreferencesKeys.BUBBLE_SIZE] ?: "medium",
            bubbleAlpha = preferences[PreferencesKeys.BUBBLE_ALPHA] ?: 1.0f,
            totalRequests = preferences[PreferencesKeys.TOTAL_REQUESTS] ?: 0,
            todayRequests = todayCount,
            geminiRequests = preferences[PreferencesKeys.GEMINI_REQUESTS] ?: 0,
            openAiRequests = preferences[PreferencesKeys.OPENAI_REQUESTS] ?: 0,
            poeRequests = preferences[PreferencesKeys.POE_REQUESTS] ?: 0,
            openRouterRequests = preferences[PreferencesKeys.OPENROUTER_REQUESTS] ?: 0,
            screenScanRequests = preferences[PreferencesKeys.SCREEN_SCAN_REQUESTS] ?: 0,
            errorCount = preferences[PreferencesKeys.ERROR_COUNT] ?: 0,
            lastRequestDate = storedDate
        )
    }

    suspend fun getSettings(): AdminSettings = settingsFlow.first()

    suspend fun updateSettings(
        defaultProvider: String? = null,
        geminiApiKey: String? = null,
        geminiModel: String? = null,
        isGeminiEnabled: Boolean? = null,
        openAiApiKey: String? = null,
        openAiModel: String? = null,
        isOpenAiEnabled: Boolean? = null,
        poeApiKey: String? = null,
        poeModel: String? = null,
        isPoeEnabled: Boolean? = null,
        poeModelsJson: String? = null,
        openRouterApiKey: String? = null,
        openRouterModel: String? = null,
        isOpenRouterEnabled: Boolean? = null,
        openRouterModelsJson: String? = null,
        isFallbackEnabled: Boolean? = null,
        isWebSearchEnabled: Boolean? = null,
        systemPrompt: String? = null,
        isScreenScanEnabled: Boolean? = null,
        isAreaScanEnabled: Boolean? = null,
        maxImageResolution: Int? = null,
        adminPin: String? = null,
        appTheme: String? = null,
        preferredLanguage: String? = null,
        bubbleStyle: String? = null,
        bubbleCustomImagePath: String? = null,
        bubblePresetIcon: String? = null,
        bubbleText: String? = null,
        bubbleGradient: String? = null,
        bubbleSize: String? = null,
        bubbleAlpha: Float? = null
    ) {
        context.dataStore.edit { preferences ->
            defaultProvider?.let { preferences[PreferencesKeys.DEFAULT_PROVIDER] = it }
            geminiApiKey?.let { preferences[PreferencesKeys.GEMINI_API_KEY] = it }
            geminiModel?.let { preferences[PreferencesKeys.GEMINI_MODEL] = it }
            isGeminiEnabled?.let { preferences[PreferencesKeys.GEMINI_ENABLED] = it }
            openAiApiKey?.let { preferences[PreferencesKeys.OPENAI_API_KEY] = it }
            openAiModel?.let { preferences[PreferencesKeys.OPENAI_MODEL] = it }
            isOpenAiEnabled?.let { preferences[PreferencesKeys.OPENAI_ENABLED] = it }
            poeApiKey?.let { preferences[PreferencesKeys.POE_API_KEY] = it }
            poeModel?.let { preferences[PreferencesKeys.POE_MODEL] = it }
            isPoeEnabled?.let { preferences[PreferencesKeys.POE_ENABLED] = it }
            poeModelsJson?.let { preferences[PreferencesKeys.POE_MODELS_JSON] = it }
            openRouterApiKey?.let { preferences[PreferencesKeys.OPENROUTER_API_KEY] = it }
            openRouterModel?.let { preferences[PreferencesKeys.OPENROUTER_MODEL] = it }
            isOpenRouterEnabled?.let { preferences[PreferencesKeys.OPENROUTER_ENABLED] = it }
            openRouterModelsJson?.let { preferences[PreferencesKeys.OPENROUTER_MODELS_JSON] = it }
            isFallbackEnabled?.let { preferences[PreferencesKeys.FALLBACK_ENABLED] = it }
            isWebSearchEnabled?.let { preferences[PreferencesKeys.WEB_SEARCH_ENABLED] = it }
            systemPrompt?.let { preferences[PreferencesKeys.SYSTEM_PROMPT] = it }
            isScreenScanEnabled?.let { preferences[PreferencesKeys.SCREEN_SCAN_ENABLED] = it }
            isAreaScanEnabled?.let { preferences[PreferencesKeys.AREA_SCAN_ENABLED] = it }
            maxImageResolution?.let { preferences[PreferencesKeys.MAX_IMAGE_RESOLUTION] = it }
            adminPin?.let { preferences[PreferencesKeys.ADMIN_PIN] = it }
            appTheme?.let { preferences[PreferencesKeys.APP_THEME] = it }
            preferredLanguage?.let { preferences[PreferencesKeys.PREFERRED_LANGUAGE] = it }
            bubbleStyle?.let { preferences[PreferencesKeys.BUBBLE_STYLE] = it }
            bubbleCustomImagePath?.let { preferences[PreferencesKeys.BUBBLE_CUSTOM_IMAGE_PATH] = it }
            bubblePresetIcon?.let { preferences[PreferencesKeys.BUBBLE_PRESET_ICON] = it }
            bubbleText?.let { preferences[PreferencesKeys.BUBBLE_TEXT] = it }
            bubbleGradient?.let { preferences[PreferencesKeys.BUBBLE_GRADIENT] = it }
            bubbleSize?.let { preferences[PreferencesKeys.BUBBLE_SIZE] = it }
            bubbleAlpha?.let { preferences[PreferencesKeys.BUBBLE_ALPHA] = it }
        }
    }

    suspend fun recordRequest(provider: String, isScreenScan: Boolean) {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        context.dataStore.edit { preferences ->
            val storedDate = preferences[PreferencesKeys.LAST_REQUEST_DATE] ?: ""
            val todayCount = if (storedDate == today) {
                (preferences[PreferencesKeys.TODAY_REQUESTS] ?: 0) + 1
            } else {
                1
            }
            preferences[PreferencesKeys.LAST_REQUEST_DATE] = today
            preferences[PreferencesKeys.TODAY_REQUESTS] = todayCount
            preferences[PreferencesKeys.TOTAL_REQUESTS] = (preferences[PreferencesKeys.TOTAL_REQUESTS] ?: 0) + 1

            if (provider.equals("gemini", ignoreCase = true)) {
                preferences[PreferencesKeys.GEMINI_REQUESTS] = (preferences[PreferencesKeys.GEMINI_REQUESTS] ?: 0) + 1
            } else if (provider.equals("openai", ignoreCase = true)) {
                preferences[PreferencesKeys.OPENAI_REQUESTS] = (preferences[PreferencesKeys.OPENAI_REQUESTS] ?: 0) + 1
            } else if (provider.equals("poe", ignoreCase = true)) {
                preferences[PreferencesKeys.POE_REQUESTS] = (preferences[PreferencesKeys.POE_REQUESTS] ?: 0) + 1
            } else if (provider.equals("openrouter", ignoreCase = true)) {
                preferences[PreferencesKeys.OPENROUTER_REQUESTS] = (preferences[PreferencesKeys.OPENROUTER_REQUESTS] ?: 0) + 1
            }

            if (isScreenScan) {
                preferences[PreferencesKeys.SCREEN_SCAN_REQUESTS] = (preferences[PreferencesKeys.SCREEN_SCAN_REQUESTS] ?: 0) + 1
            }
        }
    }

    suspend fun recordError() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ERROR_COUNT] = (preferences[PreferencesKeys.ERROR_COUNT] ?: 0) + 1
        }
    }

    suspend fun resetStats() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOTAL_REQUESTS] = 0
            preferences[PreferencesKeys.TODAY_REQUESTS] = 0
            preferences[PreferencesKeys.GEMINI_REQUESTS] = 0
            preferences[PreferencesKeys.OPENAI_REQUESTS] = 0
            preferences[PreferencesKeys.POE_REQUESTS] = 0
            preferences[PreferencesKeys.OPENROUTER_REQUESTS] = 0
            preferences[PreferencesKeys.SCREEN_SCAN_REQUESTS] = 0
            preferences[PreferencesKeys.ERROR_COUNT] = 0
        }
    }
}
