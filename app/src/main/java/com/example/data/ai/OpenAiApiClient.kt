package com.example.data.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenAiApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateContent(
        apiKey: String,
        model: String,
        systemPrompt: String,
        messages: List<AiMessage>,
        imageInlineBase64: String? = null
    ): AiResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext AiResult.Error(
                "OpenAI API key is not configured. Please add your key in the Admin Panel.",
                isQuotaOrKeyError = true
            )
        }

        val targetModel = if (model.isNotBlank()) model else "gpt-4o-mini"
        val url = "https://api.openai.com/v1/chat/completions"

        try {
            val rootJson = JSONObject()
            rootJson.put("model", targetModel)

            val messagesArray = JSONArray()

            // System prompt
            if (systemPrompt.isNotBlank()) {
                val sysObj = JSONObject()
                sysObj.put("role", "system")
                sysObj.put("content", systemPrompt)
                messagesArray.put(sysObj)
            }

            // Chat messages
            messages.forEachIndexed { index, msg ->
                val msgObj = JSONObject()
                val role = when (msg.role.lowercase()) {
                    "user" -> "user"
                    "system" -> "system"
                    else -> "assistant"
                }
                msgObj.put("role", role)

                val rawImgData = if (index == messages.lastIndex) (imageInlineBase64 ?: msg.imageBase64) else msg.imageBase64
                val imgData = rawImgData?.let { pathOrBase64 ->
                    if (pathOrBase64.isNotBlank() && (pathOrBase64.startsWith("/") || pathOrBase64.startsWith("file://"))) {
                        try {
                            val path = pathOrBase64.replace("file://", "")
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
                        pathOrBase64
                    }
                }

                if (!imgData.isNullOrBlank()) {
                    val contentArray = JSONArray()
                    val effectiveText = if (msg.text.isNotBlank()) {
                        msg.text
                    } else {
                        "Please analyze this image. If there is a question or problem, solve it accurately. If the image is blank, empty, dark, or contains nothing recognizable, clearly state in Hindi/Hinglish that the image is empty or unclear."
                    }
                    contentArray.put(JSONObject().put("type", "text").put("text", effectiveText))
                    val imgObj = JSONObject()
                    imgObj.put("type", "image_url")
                    val urlObj = JSONObject()
                    urlObj.put("url", "data:image/jpeg;base64,$imgData")
                    imgObj.put("image_url", urlObj)
                    contentArray.put(imgObj)
                    msgObj.put("content", contentArray)
                } else {
                    msgObj.put("content", msg.text)
                }

                messagesArray.put(msgObj)
            }

            if (messagesArray.length() == 0) {
                messagesArray.put(JSONObject().put("role", "user").put("content", "Hello"))
            }

            rootJson.put("messages", messagesArray)
            rootJson.put("temperature", 0.2)
            rootJson.put("max_tokens", 1024)

            val requestBody = rootJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            val (responseCode, responseSuccessful, responseString) = client.newCall(request).execute().use { response ->
                Triple(response.code, response.isSuccessful, response.body?.string() ?: "")
            }

            if (!responseSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(responseString)
                    errJson.optJSONObject("error")?.optString("message") ?: "HTTP $responseCode: $responseString"
                } catch (e: Exception) {
                    "HTTP $responseCode: $responseString"
                }
                return@withContext AiResult.Error(
                    errorMsg,
                    isQuotaOrKeyError = responseCode == 401 || responseCode == 429
                )
            }

            val respJson = JSONObject(responseString)
            val choices = respJson.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val firstChoice = choices.getJSONObject(0)
                val msg = firstChoice.optJSONObject("message")
                val text = msg?.optString("content", "") ?: ""
                if (text.isNotBlank()) {
                    return@withContext AiResult.Success(
                        text = text,
                        providerUsed = "OpenAI",
                        modelUsed = targetModel
                    )
                }
            }

            return@withContext AiResult.Error("No response content from OpenAI.")
        } catch (e: Exception) {
            Log.e("OpenAiApiClient", "Generation error", e)
            return@withContext AiResult.Error("Network error connecting to OpenAI: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun testConnection(apiKey: String, model: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val testPrompt = listOf(AiMessage(role = "user", text = "Hi! Please reply with 'OpenAI connection successful'"))
        val result = generateContent(
            apiKey = apiKey,
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
