package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Base64
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.OmniAIApplication
import com.example.data.ai.AiMessage
import com.example.data.ai.AiRepository
import com.example.data.ai.AiResult
import com.example.data.local.ChatMessage
import com.example.data.local.Conversation
import com.example.data.preferences.AdminSettings
import com.example.service.VoiceRecognitionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as OmniAIApplication
    private val chatDao = app.database.chatDao()
    private val adminPrefs = app.adminPreferences
    private val aiRepository = AiRepository(adminPrefs)
    val voiceHelper = VoiceRecognitionHelper(application)

    val adminSettings: StateFlow<AdminSettings> = adminPrefs.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminSettings())

    private val _currentConversationId = MutableStateFlow(UUID.randomUUID().toString())
    val currentConversationId: StateFlow<String> = _currentConversationId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val conversations: StateFlow<List<Conversation>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) chatDao.getAllConversations() else chatDao.searchConversations(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentMessages: StateFlow<List<ChatMessage>> = _currentConversationId.flatMapLatest { id ->
        chatDao.getMessagesForConversation(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _streamingText = MutableStateFlow<String?>(null)
    val streamingText: StateFlow<String?> = _streamingText.asStateFlow()

    private val _attachedBitmap = MutableStateFlow<Bitmap?>(null)
    val attachedBitmap: StateFlow<Bitmap?> = _attachedBitmap.asStateFlow()

    private val _attachedBitmaps = MutableStateFlow<List<Bitmap>>(emptyList())
    val attachedBitmaps: StateFlow<List<Bitmap>> = _attachedBitmaps.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isAdminUnlocked = MutableStateFlow(false)
    val isAdminUnlocked: StateFlow<Boolean> = _isAdminUnlocked.asStateFlow()

    private val _testApiResult = MutableStateFlow<Pair<String, Boolean>?>(null)
    val testApiResult: StateFlow<Pair<String, Boolean>?> = _testApiResult.asStateFlow()

    private val _poeModelsState = MutableStateFlow<List<String>>(emptyList())
    val poeModelsState: StateFlow<List<String>> = _poeModelsState.asStateFlow()

    private val _isRefreshingPoeModels = MutableStateFlow(false)
    val isRefreshingPoeModels: StateFlow<Boolean> = _isRefreshingPoeModels.asStateFlow()

    private val _openRouterModelsState = MutableStateFlow<List<String>>(emptyList())
    val openRouterModelsState: StateFlow<List<String>> = _openRouterModelsState.asStateFlow()

    private val _isRefreshingOpenRouterModels = MutableStateFlow(false)
    val isRefreshingOpenRouterModels: StateFlow<Boolean> = _isRefreshingOpenRouterModels.asStateFlow()

    private val _pendingSharedText = MutableStateFlow<String?>(null)
    val pendingSharedText: StateFlow<String?> = _pendingSharedText.asStateFlow()

    fun setPendingSharedText(text: String?) {
        _pendingSharedText.value = text
    }

    fun consumePendingSharedText(): String? {
        val current = _pendingSharedText.value
        _pendingSharedText.value = null
        return current
    }

    init {
        viewModelScope.launch {
            adminPrefs.settingsFlow.collect { settings ->
                if (settings.poeModelsJson.isNotBlank()) {
                    _poeModelsState.value = settings.poeModelsJson.split(",").filter { it.isNotBlank() }
                }
                if (settings.openRouterModelsJson.isNotBlank()) {
                    _openRouterModelsState.value = settings.openRouterModelsJson.split(",").filter { it.isNotBlank() }
                }
            }
        }
    }

    private var generationJob: Job? = null

    fun startNewChat() {
        stopGeneration()
        clearAttachedBitmaps()
        _currentConversationId.value = UUID.randomUUID().toString()
    }

    fun selectConversation(id: String) {
        stopGeneration()
        clearAttachedBitmaps()
        _currentConversationId.value = id
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun attachImageUri(uri: Uri) {
        attachMultipleImageUris(listOf(uri))
    }

    fun attachMultipleImageUris(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val loadedBitmaps = mutableListOf<Bitmap>()
                val currentCount = _attachedBitmaps.value.size
                val capacity = (10 - currentCount).coerceAtLeast(0)

                uris.take(capacity).forEach { uri ->
                    decodeSampledBitmap(context, uri, 800, 800)?.let { bmp ->
                        loadedBitmaps.add(bmp)
                    }
                }

                if (loadedBitmaps.isNotEmpty()) {
                    val updated = (_attachedBitmaps.value + loadedBitmaps).take(10)
                    _attachedBitmaps.value = updated
                    _attachedBitmap.value = updated.firstOrNull()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "${loadedBitmaps.size} photo(s) attached! (Total ${updated.size}/10)", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            if (bytes.isEmpty()) return null

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            var inSampleSize = 1
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                val halfHeight: Int = options.outHeight / 2
                val halfWidth: Int = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
        } catch (_: Exception) {
            null
        }
    }

    fun addAttachedBitmaps(bitmaps: List<Bitmap>) {
        val updated = (_attachedBitmaps.value + bitmaps).take(10)
        _attachedBitmaps.value = updated
        _attachedBitmap.value = updated.firstOrNull()
    }

    fun removeAttachedBitmapAt(index: Int) {
        val current = _attachedBitmaps.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _attachedBitmaps.value = current
            _attachedBitmap.value = current.firstOrNull()
        }
    }

    fun setAttachedBitmap(bitmap: Bitmap?) {
        if (bitmap == null) {
            clearAttachedBitmaps()
        } else {
            val updated = (_attachedBitmaps.value + bitmap).take(10)
            _attachedBitmaps.value = updated
            _attachedBitmap.value = bitmap
        }
    }

    fun clearAttachedBitmaps() {
        val oldBitmaps = _attachedBitmaps.value
        val oldSingle = _attachedBitmap.value
        _attachedBitmap.value = null
        _attachedBitmaps.value = emptyList()
        viewModelScope.launch(Dispatchers.Default) {
            (oldBitmaps + listOfNotNull(oldSingle)).distinct().forEach { bmp ->
                try {
                    if (!bmp.isRecycled) bmp.recycle()
                } catch (_: Exception) {}
            }
        }
    }

    fun pasteFromClipboard() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val bitmaps = com.example.util.ClipboardImageHelper.getBitmapsFromClipboard(context)
            if (bitmaps.isNotEmpty()) {
                addAttachedBitmaps(bitmaps)
                Toast.makeText(context, "Screenshot/Image pasted (${bitmaps.size})", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "No image found in clipboard", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun attachClipData(clipData: ClipData) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val bitmaps = com.example.util.ClipboardImageHelper.extractBitmapsFromClipData(context, clipData)
            if (bitmaps.isNotEmpty()) {
                addAttachedBitmaps(bitmaps)
                Toast.makeText(context, "Pasted ${bitmaps.size} image(s) from keyboard/clipboard", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearAttachedBitmap() {
        clearAttachedBitmaps()
    }

    fun sendMessage(promptText: String, isScan: Boolean = false) {
        val currentBitmaps = if (_attachedBitmaps.value.isNotEmpty()) {
            _attachedBitmaps.value
        } else {
            listOfNotNull(_attachedBitmap.value)
        }

        if (promptText.isBlank() && currentBitmaps.isEmpty()) return

        val text = promptText.trim()
        val images = currentBitmaps.toList()
        _attachedBitmap.value = null
        _attachedBitmaps.value = emptyList()

        val convId = _currentConversationId.value
        val primaryImage = images.firstOrNull()

        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            _isGenerating.value = true
            _statusMessage.value = when {
                images.size > 1 -> "Analyzing ${images.size} photos step-by-step..."
                images.size == 1 -> "Analyzing photo..."
                else -> "OmniAI is thinking..."
            }

            // Downsample all images to local cache files for persistent chat history display
            val imageBase64 = withContext(Dispatchers.IO) {
                if (images.isNotEmpty()) {
                    images.mapNotNull { bmp ->
                        try {
                            val maxDim = 800
                            var scaled = bmp
                            if (bmp.width > maxDim || bmp.height > maxDim) {
                                val ratio = bmp.width.toFloat() / bmp.height.toFloat()
                                val newW = if (bmp.width > bmp.height) maxDim else (maxDim * ratio).toInt()
                                val newH = if (bmp.width > bmp.height) (maxDim / ratio).toInt() else maxDim
                                scaled = Bitmap.createScaledBitmap(bmp, newW.coerceAtLeast(1), newH.coerceAtLeast(1), true)
                            }
                            val cacheFile = java.io.File(getApplication<Application>().cacheDir, "img_${java.util.UUID.randomUUID()}.jpg")
                            java.io.FileOutputStream(cacheFile).use { fos ->
                                scaled.compress(Bitmap.CompressFormat.JPEG, 70, fos)
                            }
                            if (scaled != bmp) {
                                scaled.recycle()
                            }
                            cacheFile.absolutePath
                        } catch (e: Exception) {
                            null
                        }
                    }.joinToString("|").ifBlank { null }
                } else {
                    null
                }
            }

            val userMessage = ChatMessage(
                conversationId = convId,
                role = "user",
                text = text,
                imageBase64 = imageBase64,
                isScreenScan = isScan,
                timestamp = System.currentTimeMillis()
            )

            // Ensure conversation row exists in DB
            val existing = chatDao.getConversationById(convId)
            if (existing == null) {
                chatDao.insertConversation(
                    Conversation(
                        id = convId,
                        title = if (text.isNotBlank()) text.take(32) else "Image Query (${images.size} photos)",
                        updatedAt = System.currentTimeMillis(),
                        lastMessagePreview = text.ifBlank { "Attached ${images.size} photo(s)" }
                    )
                )
            } else {
                chatDao.updateConversation(
                    existing.copy(
                        updatedAt = System.currentTimeMillis(),
                        lastMessagePreview = text.ifBlank { "Attached ${images.size} photo(s)" }
                    )
                )
            }

            // Insert user message
            chatDao.insertMessage(userMessage)

            // Gather context history with imageBase64 preservation
            val history = chatDao.getMessagesList(convId)
            val aiMessages = history.map {
                AiMessage(role = it.role, text = it.text, imageBase64 = it.imageBase64)
            }

            _streamingText.value = ""
            val result = aiRepository.askAi(
                messages = aiMessages,
                imageBitmap = primaryImage,
                imageBitmaps = images,
                isScreenScan = isScan,
                onChunk = { chunk ->
                    _streamingText.value = (_streamingText.value ?: "") + chunk
                }
            )

            _isGenerating.value = false
            _statusMessage.value = null
            _streamingText.value = null

            // Free bitmap memory immediately
            withContext(Dispatchers.Default) {
                images.forEach { bmp ->
                    try {
                        if (!bmp.isRecycled) bmp.recycle()
                    } catch (_: Exception) {}
                }
            }

            when (result) {
                is AiResult.Success -> {
                    val modelMessage = ChatMessage(
                        conversationId = convId,
                        role = "model",
                        text = result.text,
                        timestamp = System.currentTimeMillis()
                    )
                    chatDao.insertMessage(modelMessage)
                }
                is AiResult.Error -> {
                    val errorMessage = ChatMessage(
                        conversationId = convId,
                        role = "model",
                        text = "⚠️ ${result.message}",
                        isError = true,
                        timestamp = System.currentTimeMillis()
                    )
                    chatDao.insertMessage(errorMessage)
                }
            }
        }
    }

    fun regenerateLastResponse() {
        val convId = _currentConversationId.value
        viewModelScope.launch {
            val messages = chatDao.getMessagesList(convId)
            if (messages.isEmpty()) return@launch

            val lastUserMsg = messages.lastOrNull { it.role == "user" } ?: return@launch
            // Delete subsequent model message if any
            val lastMsg = messages.last()
            if (lastMsg.role == "model") {
                chatDao.deleteMessageById(lastMsg.id)
            }

            _isGenerating.value = true
            _streamingText.value = ""
            val updatedList = chatDao.getMessagesList(convId)
            val aiMessages = updatedList.map { AiMessage(role = it.role, text = it.text, imageBase64 = it.imageBase64) }

            val result = aiRepository.askAi(
                messages = aiMessages,
                isScreenScan = lastUserMsg.isScreenScan,
                onChunk = { chunk ->
                    _streamingText.value = (_streamingText.value ?: "") + chunk
                }
            )

            _isGenerating.value = false
            _streamingText.value = null
            when (result) {
                is AiResult.Success -> {
                    chatDao.insertMessage(
                        ChatMessage(
                            conversationId = convId,
                            role = "model",
                            text = result.text,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                is AiResult.Error -> {
                    chatDao.insertMessage(
                        ChatMessage(
                            conversationId = convId,
                            role = "model",
                            text = "⚠️ ${result.message}",
                            isError = true,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        generationJob = null
        _isGenerating.value = false
        _streamingText.value = null
        _statusMessage.value = null
    }

    fun triggerScreenScan(context: Context, cropRect: Rect? = null) {
        Toast.makeText(
            context,
            "Play Protect safety ke liye screen crop features ko hata diya gaya hai. Kripya normal screenshot lekar chat me directly upload karein!",
            Toast.LENGTH_LONG
        ).show()
    }

    fun renameConversation(id: String, newTitle: String) {
        viewModelScope.launch {
            chatDao.renameConversation(id, newTitle)
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            chatDao.deleteConversation(id)
            if (_currentConversationId.value == id) {
                startNewChat()
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            chatDao.clearAllConversations()
            startNewChat()
        }
    }

    // Admin Panel methods
    fun verifyAdminPin(pin: String): Boolean {
        val currentPin = adminSettings.value.adminPin
        val success = pin == currentPin
        _isAdminUnlocked.value = success
        return success
    }

    fun lockAdmin() {
        _isAdminUnlocked.value = false
    }

    fun updateAdminSettings(
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
        viewModelScope.launch {
            adminPrefs.updateSettings(
                defaultProvider = defaultProvider,
                geminiApiKey = geminiApiKey,
                geminiModel = geminiModel,
                isGeminiEnabled = isGeminiEnabled,
                openAiApiKey = openAiApiKey,
                openAiModel = openAiModel,
                isOpenAiEnabled = isOpenAiEnabled,
                poeApiKey = poeApiKey,
                poeModel = poeModel,
                isPoeEnabled = isPoeEnabled,
                poeModelsJson = poeModelsJson,
                openRouterApiKey = openRouterApiKey,
                openRouterModel = openRouterModel,
                isOpenRouterEnabled = isOpenRouterEnabled,
                openRouterModelsJson = openRouterModelsJson,
                isFallbackEnabled = isFallbackEnabled,
                isWebSearchEnabled = isWebSearchEnabled,
                systemPrompt = systemPrompt,
                isScreenScanEnabled = isScreenScanEnabled,
                isAreaScanEnabled = isAreaScanEnabled,
                maxImageResolution = maxImageResolution,
                adminPin = adminPin,
                appTheme = appTheme,
                preferredLanguage = preferredLanguage,
                bubbleStyle = bubbleStyle,
                bubbleCustomImagePath = bubbleCustomImagePath,
                bubblePresetIcon = bubblePresetIcon,
                bubbleText = bubbleText,
                bubbleGradient = bubbleGradient,
                bubbleSize = bubbleSize,
                bubbleAlpha = bubbleAlpha
            )
        }
    }

    fun setCustomBubbleImage(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val outputFile = java.io.File(context.filesDir, "custom_floating_icon_${System.currentTimeMillis()}.png")
                // Delete older files if any
                context.filesDir.listFiles { file -> file.name.startsWith("custom_floating_icon_") }?.forEach { it.delete() }

                val copied = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    java.io.FileOutputStream(outputFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    true
                } ?: false

                if (copied) {
                    adminPrefs.updateSettings(
                        bubbleCustomImagePath = outputFile.absolutePath,
                        bubbleStyle = if (adminSettings.value.bubbleStyle == "icon_only") "circle" else adminSettings.value.bubbleStyle
                    )
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Custom Floating Icon updated successfully!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to save custom icon: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun removeCustomBubbleImage(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.filesDir.listFiles { file -> file.name.startsWith("custom_floating_icon_") }?.forEach { it.delete() }
                adminPrefs.updateSettings(bubbleCustomImagePath = "")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Reset to default AI icon", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {}
        }
    }

    fun testGeminiConnection(apiKey: String, model: String) {
        viewModelScope.launch {
            _testApiResult.value = Pair("Testing Gemini connection...", false)
            val result = aiRepository.testGemini(apiKey, model)
            _testApiResult.value = Pair(result.second, result.first)
        }
    }

    fun testOpenAiConnection(apiKey: String, model: String) {
        viewModelScope.launch {
            _testApiResult.value = Pair("Testing OpenAI connection...", false)
            val result = aiRepository.testOpenAi(apiKey, model)
            _testApiResult.value = Pair(result.second, result.first)
        }
    }

    fun testPoeConnection(apiKey: String, model: String) {
        viewModelScope.launch {
            _testApiResult.value = Pair("Testing Poe connection...", false)
            val result = aiRepository.testPoe(apiKey, model)
            _testApiResult.value = Pair(result.second, result.first)
        }
    }

    fun refreshPoeModels(apiKey: String) {
        if (apiKey.isBlank()) return
        viewModelScope.launch {
            _isRefreshingPoeModels.value = true
            try {
                val models = aiRepository.fetchPoeModels(apiKey)
                if (models.isNotEmpty()) {
                    _poeModelsState.value = models
                    val modelsString = models.joinToString(",")
                    adminPrefs.updateSettings(poeModelsJson = modelsString)
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error refreshing models", e)
            } finally {
                _isRefreshingPoeModels.value = false
            }
        }
    }

    fun testOpenRouterConnection(apiKey: String, model: String) {
        viewModelScope.launch {
            _testApiResult.value = Pair("Testing OpenRouter connection...", false)
            val result = aiRepository.testOpenRouter(apiKey, model)
            _testApiResult.value = Pair(result.second, result.first)
        }
    }

    fun refreshOpenRouterModels(apiKey: String) {
        viewModelScope.launch {
            _isRefreshingOpenRouterModels.value = true
            try {
                val models = aiRepository.fetchOpenRouterModels(apiKey)
                if (models.isNotEmpty()) {
                    _openRouterModelsState.value = models
                    val modelsString = models.joinToString(",")
                    adminPrefs.updateSettings(openRouterModelsJson = modelsString)
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error refreshing OpenRouter models", e)
            } finally {
                _isRefreshingOpenRouterModels.value = false
            }
        }
    }

    fun resetStats() {
        viewModelScope.launch {
            adminPrefs.resetStats()
        }
    }
}
