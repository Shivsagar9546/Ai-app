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

class GeminiApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

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

        // Smart Adaptive Model Routing & Modern fallbacks (avoiding deprecated gemini-1.5 models)
        val latestUserQuery = messages.lastOrNull { it.role.equals("user", ignoreCase = true) }?.text ?: ""
        val resolvedModel = if (model == "auto" || model.isBlank()) {
            autoSelectModel(latestUserQuery, hasImages = allImages.isNotEmpty())
        } else {
            model
        }

        val requestedModel = if (resolvedModel.isNotBlank()) resolvedModel else "gemini-3.5-flash"
        val modelsToTry = mutableListOf<String>().apply {
            add(requestedModel)
            if (requestedModel != "gemini-3.5-flash") add("gemini-3.5-flash")
            if (requestedModel != "gemini-flash-latest") add("gemini-flash-latest")
            if (requestedModel != "gemini-3.1-pro-preview") add("gemini-3.1-pro-preview")
            if (requestedModel != "gemini-3.1-flash-lite-preview") add("gemini-3.1-flash-lite-preview")
        }.distinct()

        var lastErrorMsg = "Unable to reach Gemini servers."
        var isQuotaOrKey = false

        for (targetModel in modelsToTry) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$targetModel:generateContent?key=$key"
            try {
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

                    // Attach images for this message
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
                        "Please examine the attached image carefully:\n1. If the image is empty, blank, dark, blurry, or contains no readable text, question, or clear subject, politely respond in simple Hindi/Hinglish: 'इस इमेज में कोई स्पष्ट प्रश्न या कंटेंट दिखाई नहीं दे रहा है। कृपया किसी प्रश्न या विषय की साफ़ फोटो अपलोड करें या बताएं कि मैं आपकी क्या मदद कर सकता हूँ।'\n2. Answer or explain whatever is in the image according to its real topic (science, general, history, biology, notes, coding, art, etc.). Do NOT force mathematical formatting or equations unless the image actually contains math or numbers.\n3. Keep the explanation natural, clear, accurate, and easy to understand in Hindi/Hinglish."
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

                // If empty, add default prompt
                if (contentsArray.length() == 0) {
                    val contentObj = JSONObject()
                    contentObj.put("role", "user")
                    val partsArray = JSONArray()
                    partsArray.put(JSONObject().put("text", "Hello"))
                    contentObj.put("parts", partsArray)
                    contentsArray.put(contentObj)
                }

                rootJson.put("contents", contentsArray)

                // Generation config: Low temperature for direct, fast, accurate responses without hallucinations
                val genConfig = JSONObject()
                genConfig.put("temperature", 0.1)
                genConfig.put("topP", 0.85)
                genConfig.put("maxOutputTokens", 4096)

                rootJson.put("generationConfig", genConfig)

                // If web search is enabled, add google_search_retrieval tool for real-time grounding
                if (isWebSearchEnabled) {
                    val toolsArray = JSONArray()
                    val googleSearchObj = JSONObject()
                    googleSearchObj.put("google_search_retrieval", JSONObject())
                    toolsArray.put(googleSearchObj)
                    rootJson.put("tools", toolsArray)
                }

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

                    // Try next model if this is a model error (like 404/400) or high demand/overload
                    if (!isKeyError && !isQuotaError && targetModel != modelsToTry.last()) {
                        Log.w("GeminiApiClient", "Model $targetModel failed ($errorMsg). Trying next fallback model...")
                        kotlinx.coroutines.delay(200)
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
                        // Parse Grounding Metadata for real-time web search links
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

                // If candidate was empty, try next model
                if (targetModel != modelsToTry.last()) {
                    continue
                }
                return@withContext AiResult.Error("No response generated from Gemini.")
            } catch (e: Exception) {
                Log.e("GeminiApiClient", "Generation error on $targetModel", e)
                lastErrorMsg = "Network error connecting to Gemini: ${e.localizedMessage ?: e.message}"
                if (targetModel != modelsToTry.last()) {
                    kotlinx.coroutines.delay(300)
                    continue
                }
            }
        }

        return@withContext AiResult.Error(lastErrorMsg, isQuotaOrKeyError = isQuotaOrKey)
    }

    private fun autoSelectModel(userPrompt: String, hasImages: Boolean = false): String {
        if (hasImages) {
            return "gemini-3.5-flash"
        }
        val text = userPrompt.lowercase().trim()
        
        // Define lists of indicators for complex or mathematical topics
        val complexOrMathKeywords = listOf(
            "solve", "equation", "math", "calculus", "derivative", "matrix", "integral", "geometry",
            "trigonometry", "algebra", "sin", "cos", "tan", "theta", "θ", "fraction", "formula",
            "proof", "theorem", "मान ज्ञात", "समीकरण", "गणित", "हल करें", "मान निकालें", "गुणनखंड"
        )
        
        // Define indicators for programming or coding questions
        val codingKeywords = listOf(
            "code", "program", "function", "write a class", "database", "sql", "html", "css",
            "javascript", "java", "kotlin", "python", "c++", "c#", "rust", "compile", "bug", "error in line",
            "syntax", "api", "json", "xml", "git", "github", "कोडिंग", "प्रोग्राम"
        )
        
        // Define common short greetings or simple questions
        val simpleKeywords = listOf(
            "hi", "hello", "hey", "hola", "greetings", "good morning", "good evening",
            "how are you", "who are you", "who made you", "what is your name", "नमस्ते", "हैलो", "कैसे हो", "कौन हो"
        )

        val hasComplexKeyword = complexOrMathKeywords.any { text.contains(it) }
        val hasCodingKeyword = codingKeywords.any { text.contains(it) }
        val isVeryLongQuery = text.split("\\s+".toRegex()).size > 80 // Long detailed prompts
        
        return when {
            hasComplexKeyword || hasCodingKeyword || isVeryLongQuery -> {
                "gemini-3.1-pro-preview"
            }
            simpleKeywords.any { text == it || text.startsWith(it + " ") || text.endsWith(" " + it) } || text.split("\\s+".toRegex()).size < 5 -> {
                "gemini-3.1-flash-lite-preview"
            }
            else -> {
                "gemini-3.5-flash"
            }
        }
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
            is AiResult.Success -> Pair(true, "Success: ${result.text.take(80)}")
            is AiResult.Error -> Pair(false, result.message)
        }
    }
}
