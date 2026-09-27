package com.example.data.ai

import android.graphics.Bitmap
import android.util.Base64
import com.example.data.preferences.AdminPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class AiRepository(
    private val adminPreferencesRepository: AdminPreferencesRepository,
    private val geminiApiClient: GeminiApiClient = GeminiApiClient(),
    private val openAiApiClient: OpenAiApiClient = OpenAiApiClient(),
    private val poeApiClient: PoeApiClient = PoeApiClient(),
    private val openRouterApiClient: OpenRouterApiClient = OpenRouterApiClient()
) {

    suspend fun askAi(
        messages: List<AiMessage>,
        imageBitmap: Bitmap? = null,
        imageBitmaps: List<Bitmap> = emptyList(),
        isScreenScan: Boolean = false,
        systemPromptOverride: String? = null
    ): AiResult = withContext(Dispatchers.IO) {
        val settings = adminPreferencesRepository.getSettings()
        val advanced = com.example.data.preferences.AdvancedSettingsHelper(adminPreferencesRepository.context)

        // Check Maintenance Mode
        if (advanced.isMaintenanceMode) {
            return@withContext AiResult.Error("The Screen Assistant is currently undergoing maintenance. Please try again later.")
        }

        // Check Daily Quota Limit
        if (settings.todayRequests >= advanced.dailyQuotaLimit) {
            return@withContext AiResult.Error("Daily scan quota limit of ${advanced.dailyQuotaLimit} reached. Please contact your administrator.")
        }

        // Check if screen scan is disabled by admin
        if (isScreenScan && !settings.isScreenScanEnabled) {
            return@withContext AiResult.Error("Screen scan has been disabled by the administrator in Admin Settings.")
        }

        // Collect all images (either single bitmap or list of bitmaps), filtering out any recycled bitmaps
        val allBitmaps = when {
            imageBitmaps.isNotEmpty() -> imageBitmaps.filter { !it.isRecycled }
            imageBitmap != null && !imageBitmap.isRecycled -> listOf(imageBitmap)
            else -> emptyList()
        }

        // Fast parallel image compression: 1024px maximum dimension with 75% JPEG quality
        // Reduces upload payload by ~95%, allowing instant transfer and ultra-fast Gemini OCR analysis
        val compressedImagesBase64 = if (allBitmaps.isNotEmpty()) {
            coroutineScope {
                allBitmaps.mapNotNull { bmp ->
                    async(Dispatchers.Default) {
                        try {
                            compressBitmapToBase64(bmp, settings.maxImageResolution).takeIf { it.isNotBlank() }
                        } catch (_: Exception) {
                            null
                        }
                    }
                }.awaitAll().filterNotNull()
            }
        } else {
            emptyList()
        }

        val firstImageBase64 = compressedImagesBase64.firstOrNull()
        val primaryProvider = settings.defaultProvider.lowercase()
        val sysPrompt = systemPromptOverride ?: settings.systemPrompt

        // Prepare Gemini Keys to try
        val geminiKeys = mutableListOf<String>()
        if (settings.geminiApiKey.isNotBlank()) {
            geminiKeys.add(settings.geminiApiKey)
        }
        if (advanced.geminiApiKey2.isNotBlank()) {
            geminiKeys.add(advanced.geminiApiKey2)
        }
        if (advanced.geminiApiKey3.isNotBlank()) {
            geminiKeys.add(advanced.geminiApiKey3)
        }
        if (advanced.geminiApiKey4.isNotBlank()) {
            geminiKeys.add(advanced.geminiApiKey4)
        }
        if (advanced.geminiApiKey5.isNotBlank()) {
            geminiKeys.add(advanced.geminiApiKey5)
        }
        if (geminiKeys.isEmpty()) {
            geminiKeys.add("") // try fallback / default key
        }

        var finalResult: AiResult? = null
        val startTime = System.currentTimeMillis()

        // Try primary provider
        if (primaryProvider == "openai") {
            finalResult = openAiApiClient.generateContent(
                apiKey = settings.openAiApiKey,
                model = settings.openAiModel,
                systemPrompt = sysPrompt,
                messages = messages,
                imageInlineBase64 = firstImageBase64
            )
        } else if (primaryProvider == "poe") {
            finalResult = poeApiClient.generateContent(
                apiKey = settings.poeApiKey,
                model = settings.poeModel,
                systemPrompt = sysPrompt,
                messages = messages,
                imageInlineBase64 = firstImageBase64
            )
        } else if (primaryProvider == "openrouter") {
            finalResult = openRouterApiClient.generateContent(
                apiKey = settings.openRouterApiKey,
                model = settings.openRouterModel,
                systemPrompt = sysPrompt,
                messages = messages,
                imageInlineBase64 = firstImageBase64
            )
        } else {
            // Try Gemini keys in sequence for automatic key rotation failover
            for (keyToTry in geminiKeys) {
                val geminiResult = geminiApiClient.generateContent(
                    apiKeyOverride = keyToTry.ifBlank { null },
                    model = settings.geminiModel,
                    systemPrompt = sysPrompt,
                    messages = messages,
                    imageInlineBase64 = firstImageBase64,
                    imagesInlineBase64 = compressedImagesBase64,
                    isWebSearchEnabled = settings.isWebSearchEnabled
                )
                if (geminiResult is AiResult.Success) {
                    finalResult = geminiResult
                    break
                } else {
                    finalResult = geminiResult // save error to return if all fail
                }
            }
        }

        // If primary failed and fallback is enabled, try the alternative
        if (finalResult !is AiResult.Success && settings.isFallbackEnabled) {
            if (primaryProvider == "openai" || primaryProvider == "poe" || primaryProvider == "openrouter") {
                // Fallback to Gemini with key rotation
                for (keyToTry in geminiKeys) {
                    val fallbackResult = geminiApiClient.generateContent(
                        apiKeyOverride = keyToTry.ifBlank { null },
                        model = settings.geminiModel,
                        systemPrompt = sysPrompt,
                        messages = messages,
                        imageInlineBase64 = firstImageBase64,
                        imagesInlineBase64 = compressedImagesBase64,
                        isWebSearchEnabled = settings.isWebSearchEnabled
                    )
                    if (fallbackResult is AiResult.Success) {
                        finalResult = fallbackResult
                        break
                    } else {
                        finalResult = fallbackResult
                    }
                }
            } else {
                // Fallback to OpenAI
                finalResult = openAiApiClient.generateContent(
                    apiKey = settings.openAiApiKey,
                    model = settings.openAiModel,
                    systemPrompt = sysPrompt,
                    messages = messages,
                    imageInlineBase64 = firstImageBase64
                )
            }
        }

        if (finalResult is AiResult.Success) {
            val latency = System.currentTimeMillis() - startTime
            try {
                advanced.lastApiLatencyMs = latency
            } catch (e: Exception) {}
            adminPreferencesRepository.recordRequest(finalResult.providerUsed, isScreenScan)
            return@withContext finalResult
        }

        // If all failed, record error and return error
        adminPreferencesRepository.recordError()
        val errorMsg = (finalResult as? AiResult.Error)?.message ?: "Unable to complete AI request."
        return@withContext AiResult.Error(errorMsg)
    }

    private fun compressBitmapToBase64(bitmap: Bitmap, maxDim: Int): String {
        if (bitmap.isRecycled) return ""
        try {
            // Use 1024 maxDim for ultra-fast network transfer & low latency without sacrificing OCR clarity
            val targetMaxDim = if (maxDim in 480..1280) maxDim else 1024
            var scaledBitmap = bitmap
            val width = bitmap.width
            val height = bitmap.height

            if (width > targetMaxDim || height > targetMaxDim) {
                val ratio = width.toFloat() / height.toFloat()
                val newWidth: Int
                val newHeight: Int
                if (width > height) {
                    newWidth = targetMaxDim
                    newHeight = (targetMaxDim / ratio).toInt().coerceAtLeast(1)
                } else {
                    newHeight = targetMaxDim
                    newWidth = (targetMaxDim * ratio).toInt().coerceAtLeast(1)
                }
                scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            }

            val outputStream = ByteArrayOutputStream()
            // 75% JPEG gives sharp OCR reading with tiny ~80KB payload for instant response
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            if (scaledBitmap != bitmap) {
                try {
                    scaledBitmap.recycle()
                } catch (_: Exception) {}
            }
            val byteArray = outputStream.toByteArray()
            return Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (_: Exception) {
            return ""
        }
    }

    suspend fun testGemini(apiKey: String, model: String) = geminiApiClient.testConnection(apiKey, model)

    suspend fun testOpenAi(apiKey: String, model: String) = openAiApiClient.testConnection(apiKey, model)

    suspend fun testPoe(apiKey: String, model: String) = poeApiClient.testConnection(apiKey, model)

    suspend fun fetchPoeModels(apiKey: String) = poeApiClient.fetchAvailableModels(apiKey)

    suspend fun testOpenRouter(apiKey: String, model: String) = openRouterApiClient.testConnection(apiKey, model)

    suspend fun fetchOpenRouterModels(apiKey: String) = openRouterApiClient.fetchAvailableModels(apiKey)
}
