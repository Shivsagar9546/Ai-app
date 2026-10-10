package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiModelRegistry {
    val ACTIVE_MODELS = listOf(
        "gemini-2.5-flash",
        "gemini-2.0-flash",
        "gemini-2.5-pro",
        "gemini-2.0-flash-lite",
        "gemini-1.5-flash",
        "gemini-1.5-pro"
    )

    val DEPRECATED_OR_INVALID_MODELS = setOf(
        "gemini-1.0-pro",
        "gemini-1.0-pro-vision",
        "gemini-pro",
        "gemini-pro-vision",
        "gemini-3.5-flash",
        "gemini-flash-latest",
        "gemini-pro-latest"
    )

    fun isDeprecated(model: String): Boolean {
        val lower = model.trim().lowercase()
        return DEPRECATED_OR_INVALID_MODELS.contains(lower) || lower.startsWith("gemini-1.0")
    }

    fun buildFallbackChain(requestedModel: String): List<String> {
        val chain = mutableListOf<String>()
        val cleanRequested = requestedModel.trim().lowercase()

        if (cleanRequested.isNotBlank() && cleanRequested != "auto" && !isDeprecated(cleanRequested)) {
            chain.add(cleanRequested)
        }

        // Chain all official active Gemini models in priority order
        ACTIVE_MODELS.forEach { model ->
            if (!chain.contains(model)) {
                chain.add(model)
            }
        }
        return chain
    }
}

class GeminiApiClient {

    private val client = OkHttpClient.Builder()
        .connectionPool(okhttp3.ConnectionPool(16, 10, TimeUnit.MINUTES))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun buildPayloadJson(
        systemPrompt: String,
        messages: List<AiMessage>,
        allImages: List<String>,
        isWebSearchEnabled: Boolean
    ): JSONObject {
        val rootJson = JSONObject()

        // System Instruction
        if (systemPrompt.isNotBlank()) {
            val sysInst = JSONObject()
            val sysParts = JSONArray()
            sysParts.put(JSONObject().put("text", systemPrompt))
            sysInst.put("parts", sysParts)
            rootJson.put("systemInstruction", sysInst)
        }

        // Contents
        val contentsArray = JSONArray()
        messages.forEachIndexed { index, msg ->
            val contentObj = JSONObject()
            val isUser = msg.role.equals("user", ignoreCase = true)
            contentObj.put("role", if (isUser) "user" else "model")

            val messageImages = if (index == messages.lastIndex) {
                if (allImages.isNotEmpty()) allImages
                else if (!msg.imageBase64.isNullOrBlank()) listOf(msg.imageBase64)
                else emptyList()
            } else {
                if (!msg.imageBase64.isNullOrBlank()) listOf(msg.imageBase64)
                else emptyList()
            }

            val resolvedImages = messageImages.flatMap { imgData ->
                if (imgData.contains("|")) {
                    imgData.split("|")
                } else {
                    listOf(imgData)
                }
            }.mapNotNull { imgData ->
                if (imgData.isNotBlank() && (imgData.startsWith("/") || imgData.startsWith("file://"))) {
                    try {
                        val path = imgData.replace("file://", "")
                        val file = java.io.File(path)
                        if (file.exists()) {
                            val bytes = file.readBytes()
                            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    imgData
                }
            }

            val partsArray = JSONArray()
            val effectiveText = if (msg.text.isNotBlank()) {
                msg.text
            } else if (isUser && resolvedImages.isNotEmpty()) {
                "कृपया इस इमेज को ध्यान से समझें और निम्नलिखित नियमों के अनुसार हल करें:\n1. शुरुआत में 'सही उत्तर है: Option [A/B/C/D] [Value]' लिखें।\n2. केवल जरूरत के अनुसार Step-by-Step point-to-point हल दें।\n3. Physics/Math की सभी equations proper LaTeX में दें (\$ या \$\$) और हर मुख्य गणना अलग line में रखें। कभी भी formula को code block में न डालें।\n4. अंत में 'Final Answer: Option [A/B/C/D] [Value]' दें।\n5. भाषा बहुत सरल Hindi/Hinglish रखें।"
            } else {
                ""
            }
            if (effectiveText.isNotBlank()) {
                partsArray.put(JSONObject().put("text", effectiveText))
            }

            resolvedImages.forEach { imgData ->
                if (imgData.isNotBlank()) {
                    val inlineDataObj = JSONObject()
                    inlineDataObj.put("mimeType", "image/jpeg")
                    inlineDataObj.put("data", imgData)
                    partsArray.put(JSONObject().put("inlineData", inlineDataObj))
                }
            }

            if (partsArray.length() > 0) {
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
            }
        }

        if (contentsArray.length() == 0) {
            val contentObj = JSONObject()
            contentObj.put("role", "user")
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", "Hello"))
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
        }

        rootJson.put("contents", contentsArray)

        // Generation config: Ultra-fast TTFT with high accuracy
        val genConfig = JSONObject()
        genConfig.put("temperature", 0.3)
        genConfig.put("topP", 0.95)
        genConfig.put("maxOutputTokens", 4096)
        rootJson.put("generationConfig", genConfig)

        if (isWebSearchEnabled) {
            val toolsArray = JSONArray()
            val googleSearchObj = JSONObject()
            googleSearchObj.put("google_search_retrieval", JSONObject())
            toolsArray.put(googleSearchObj)
            rootJson.put("tools", toolsArray)
        }

        return rootJson
    }

    suspend fun generateContentStream(
        apiKeyOverride: String?,
        model: String,
        systemPrompt: String,
        messages: List<AiMessage>,
        imageInlineBase64: String? = null,
        imagesInlineBase64: List<String> = emptyList(),
        isWebSearchEnabled: Boolean = false,
        onChunk: (String) -> Unit
    ): AiResult = withContext(Dispatchers.IO) {
        val key = if (!apiKeyOverride.isNullOrBlank()) {
            apiKeyOverride
        } else {
            try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Exception) {
                ""
            }
        }

        if (key.isBlank() || key == "MY_GEMINI_API_KEY") {
            return@withContext AiResult.Error(
                "Gemini API key is not configured. Please configure it in the Admin Panel or through AI Studio Secrets.",
                isQuotaOrKeyError = true
            )
        }

        val allImages = when {
            imagesInlineBase64.isNotEmpty() -> imagesInlineBase64
            !imageInlineBase64.isNullOrBlank() -> listOf(imageInlineBase64)
            else -> emptyList()
        }

        val latestUserQuery = messages.lastOrNull { it.role.equals("user", ignoreCase = true) }?.text ?: ""
        val resolvedModel = if (model == "auto" || model.isBlank() || GeminiModelRegistry.isDeprecated(model)) {
            autoSelectModel(latestUserQuery, hasImages = allImages.isNotEmpty())
        } else {
            model
        }

        val modelsToTry = GeminiModelRegistry.buildFallbackChain(resolvedModel)
        var lastErrorMsg = "Unable to reach Gemini servers."
        var isQuotaOrKey = false

        for (targetModel in modelsToTry) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$targetModel:streamGenerateContent?alt=sse&key=$key"
            try {
                val rootJson = buildPayloadJson(systemPrompt, messages, allImages, isWebSearchEnabled)
                val requestBody = rootJson.toString().toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val fullTextBuilder = StringBuilder()
                var hasReceivedAnyChunk = false

                val response = client.newCall(request).execute()
                val responseCode = response.code

                if (responseCode !in 200..299) {
                    val errorString = response.body?.string() ?: ""
                    val errorMsg = try {
                        val errorJson = JSONObject(errorString)
                        errorJson.optJSONObject("error")?.optString("message") ?: "HTTP $responseCode: $errorString"
                    } catch (e: Exception) {
                        "HTTP $responseCode: $errorString"
                    }
                    response.close()
                    lastErrorMsg = errorMsg
                    val isKeyError = responseCode == 401 || responseCode == 403
                    val isQuotaError = responseCode == 429
                    isQuotaOrKey = isKeyError || isQuotaError

                    Log.w("GeminiApiClient", "Model $targetModel failed ($responseCode: $errorMsg). Auto-falling back to next model...")
                    if (targetModel != modelsToTry.last()) {
                        continue
                    }
                    return@withContext AiResult.Error(errorMsg, isQuotaOrKeyError = isQuotaOrKey)
                }

                response.body?.byteStream()?.bufferedReader()?.use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val trimmed = line?.trim() ?: continue
                        if (!trimmed.startsWith("data:")) continue
                        val jsonPayload = trimmed.removePrefix("data:").trim()
                        if (jsonPayload.isEmpty() || jsonPayload == "[DONE]") continue

                        try {
                            val chunkObj = JSONObject(jsonPayload)
                            val candidates = chunkObj.optJSONArray("candidates")
                            if (candidates != null && candidates.length() > 0) {
                                val firstCandidate = candidates.getJSONObject(0)
                                val content = firstCandidate.optJSONObject("content")
                                val parts = content?.optJSONArray("parts")
                                if (parts != null) {
                                    for (i in 0 until parts.length()) {
                                        val part = parts.getJSONObject(i)
                                        val chunkText = part.optString("text", "")
                                        if (chunkText.isNotEmpty()) {
                                            fullTextBuilder.append(chunkText)
                                            hasReceivedAnyChunk = true
                                            withContext(Dispatchers.Main) {
                                                onChunk(chunkText)
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }

                val finalOutput = fullTextBuilder.toString()
                if (finalOutput.isNotBlank() || hasReceivedAnyChunk) {
                    return@withContext AiResult.Success(
                        text = finalOutput,
                        providerUsed = "Gemini",
                        modelUsed = targetModel
                    )
                }

                if (targetModel != modelsToTry.last()) {
                    continue
                }
                return@withContext AiResult.Error("No response generated from Gemini stream.")
            } catch (e: Exception) {
                Log.e("GeminiApiClient", "Streaming error on $targetModel", e)
                lastErrorMsg = "Network error: ${e.localizedMessage ?: e.message}"
                if (targetModel != modelsToTry.last()) {
                    continue
                }
            }
        }

        return@withContext AiResult.Error(lastErrorMsg, isQuotaOrKeyError = isQuotaOrKey)
    }

    suspend fun generateContent(
        apiKeyOverride: String?,
        model: String,
        systemPrompt: String,
        messages: List<AiMessage>,
        imageInlineBase64: String? = null,
        imagesInlineBase64: List<String> = emptyList(),
        isWebSearchEnabled: Boolean = false
    ): AiResult = withContext(Dispatchers.IO) {
        val key = if (!apiKeyOverride.isNullOrBlank()) {
            apiKeyOverride
        } else {
            try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Exception) {
                ""
            }
        }

        if (key.isBlank() || key == "MY_GEMINI_API_KEY") {
            return@withContext AiResult.Error(
                "Gemini API key is not configured. Please configure it in the Admin Panel or through AI Studio Secrets.",
                isQuotaOrKeyError = true
            )
        }

        val allImages = when {
            imagesInlineBase64.isNotEmpty() -> imagesInlineBase64
            !imageInlineBase64.isNullOrBlank() -> listOf(imageInlineBase64)
            else -> emptyList()
        }

        val latestUserQuery = messages.lastOrNull { it.role.equals("user", ignoreCase = true) }?.text ?: ""
        val resolvedModel = if (model == "auto" || model.isBlank() || GeminiModelRegistry.isDeprecated(model)) {
            autoSelectModel(latestUserQuery, hasImages = allImages.isNotEmpty())
        } else {
            model
        }

        val modelsToTry = GeminiModelRegistry.buildFallbackChain(resolvedModel)
        var lastErrorMsg = "Unable to reach Gemini servers."
        var isQuotaOrKey = false

        for (targetModel in modelsToTry) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$targetModel:generateContent?key=$key"
            try {
                val rootJson = buildPayloadJson(systemPrompt, messages, allImages, isWebSearchEnabled)
                val requestBody = rootJson.toString().toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val (responseCode, responseString) = client.newCall(request).execute().use { resp ->
                    Pair(resp.code, resp.body?.string() ?: "")
                }

                if (responseCode !in 200..299) {
                    val errorMsg = try {
                        val errorJson = JSONObject(responseString)
                        errorJson.optJSONObject("error")?.optString("message") ?: "HTTP $responseCode: $responseString"
                    } catch (e: Exception) {
                        "HTTP $responseCode: $responseString"
                    }
                    lastErrorMsg = errorMsg
                    
                    val isKeyError = responseCode == 401 || responseCode == 403
                    val isQuotaError = responseCode == 429
                    isQuotaOrKey = isKeyError || isQuotaError

                    Log.w("GeminiApiClient", "Model $targetModel failed ($responseCode: $errorMsg). Auto-falling back to next model...")
                    if (targetModel != modelsToTry.last()) {
                        continue
                    }

                    return@withContext AiResult.Error(
                        errorMsg,
                        isQuotaOrKeyError = isQuotaOrKey
                    )
                }

                val respJson = JSONObject(responseString)
                val candidates = respJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val textBuilder = StringBuilder()
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            textBuilder.append(part.optString("text", ""))
                        }
                    }
                    var text = textBuilder.toString()
                    if (text.isNotBlank()) {
                        val groundingMetadata = firstCandidate.optJSONObject("groundingMetadata")
                        if (groundingMetadata != null) {
                            val groundingChunks = groundingMetadata.optJSONArray("groundingChunks")
                            if (groundingChunks != null && groundingChunks.length() > 0) {
                                val sourcesList = mutableListOf<String>()
                                for (j in 0 until groundingChunks.length()) {
                                    val chunk = groundingChunks.getJSONObject(j)
                                    val web = chunk.optJSONObject("web")
                                    if (web != null) {
                                        val title = web.optString("title", "")
                                        val uri = web.optString("uri", "")
                                        if (uri.isNotBlank()) {
                                            val displayTitle = if (title.isNotBlank()) title else uri
                                            sourcesList.add("- [$displayTitle]($uri)")
                                        }
                                    }
                                }
                                if (sourcesList.isNotEmpty()) {
                                    val distinctSources = sourcesList.distinct().take(5)
                                    text += "\n\n🌐 **Web Search Sources:**\n" + distinctSources.joinToString("\n")
                                }
                            }
                        }

                        return@withContext AiResult.Success(
                            text = text,
                            providerUsed = "Gemini",
                            modelUsed = targetModel
                        )
                    }
                }

                if (targetModel != modelsToTry.last()) {
                    continue
                }
                return@withContext AiResult.Error("No response generated from Gemini.")
            } catch (e: Exception) {
                Log.e("GeminiApiClient", "Generation error on $targetModel", e)
                lastErrorMsg = "Network error connecting to Gemini: ${e.localizedMessage ?: e.message}"
                if (targetModel != modelsToTry.last()) {
                    continue
                }
            }
        }

        return@withContext AiResult.Error(lastErrorMsg, isQuotaOrKeyError = isQuotaOrKey)
    }

    private fun autoSelectModel(userPrompt: String, hasImages: Boolean = false): String {
        val lower = userPrompt.lowercase()
        val isDeepReasoning = lower.contains("derive") || lower.contains("proof") || 
                              lower.contains("integration") || lower.contains("differential") ||
                              lower.contains("calculate the ratio") || lower.contains("jee advanced")
        return if (isDeepReasoning) "gemini-2.5-pro" else "gemini-2.5-flash"
    }

    suspend fun testConnection(apiKey: String, model: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val testPrompt = listOf(AiMessage(role = "user", text = "Hi! Please reply with 'Gemini connection successful'"))
        val result = generateContent(
            apiKeyOverride = apiKey,
            model = model,
            systemPrompt = "You are a test agent. Keep answer short.",
            messages = testPrompt
        )
        when (result) {
            is AiResult.Success -> Pair(true, "Success (Model: ${result.modelUsed}): ${result.text.take(80)}")
            is AiResult.Error -> Pair(false, result.message)
        }
    }
}
