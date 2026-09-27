package com.example.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

object TtsManager {
    private const val TAG = "TtsManager"
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentSpeakingId = MutableStateFlow<String?>(null)
    val currentSpeakingId: StateFlow<String?> = _currentSpeakingId.asStateFlow()

    fun init(context: Context) {
        if (textToSpeech != null && isInitialized) return

        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                try {
                    val result = textToSpeech?.setLanguage(Locale.forLanguageTag("hi-IN"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        textToSpeech?.language = Locale.ENGLISH
                    }
                } catch (_: Exception) {
                    try { textToSpeech?.language = Locale.getDefault() } catch (_: Exception) {}
                }
                try {
                    textToSpeech?.setSpeechRate(0.95f)
                    textToSpeech?.setPitch(1.0f)
                } catch (_: Exception) {}

                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                        _currentSpeakingId.value = utteranceId
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        _currentSpeakingId.value = null
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        _currentSpeakingId.value = null
                    }
                })
            } else {
                Log.e(TAG, "Failed to initialize TextToSpeech engine")
            }
        }
    }

    fun speak(text: String, id: String = System.currentTimeMillis().toString()) {
        if (textToSpeech == null || !isInitialized) return

        // Clean markdown and formatting symbols for natural speech
        val cleanSpeech = text
            .replace(Regex("```[a-zA-Z]*"), "")
            .replace("```", "")
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("#+\\s*"), "")
            .replace(Regex("[$]{1,2}"), "")
            .replace(Regex("[#*_`~]"), "")
            .trim()

        if (cleanSpeech.isBlank()) return

        // Stop previous speech if any
        stop()

        _currentSpeakingId.value = id
        _isSpeaking.value = true
        try {
            textToSpeech?.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, null, id)
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking text", e)
            _isSpeaking.value = false
            _currentSpeakingId.value = null
        }
    }

    fun stop() {
        try {
            textToSpeech?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
        _isSpeaking.value = false
        _currentSpeakingId.value = null
    }

    fun shutdown() {
        stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
    }
}
