package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.local.AppDatabase
import com.example.data.preferences.AdminPreferencesRepository

class OmniAIApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var adminPreferences: AdminPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)
        adminPreferences = AdminPreferencesRepository(this)

        createNotificationChannels()
        com.example.service.TtsManager.init(this)

        // Global crash guard to prevent unexpected background exceptions from crashing the app
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("OmniAI", "Uncaught exception on thread ${thread.name}", throwable)
            val msg = throwable.message ?: ""
            val className = throwable.javaClass.name
            val isRecoverableGlitch = throwable is android.view.WindowManager.BadTokenException ||
                    throwable is android.view.WindowManager.InvalidDisplayException ||
                    (throwable is IllegalArgumentException && (msg.contains("not attached") || msg.contains("View=") || msg.contains("has already been added") || msg.contains("Service not registered"))) ||
                    (throwable is IllegalStateException && (msg.contains("ActivityResultRegistryOwner") || msg.contains("ForegroundService") || msg.contains("MediaProjection") || msg.contains("Lifecycle"))) ||
                    className.contains("ForegroundService") ||
                    className.contains("BadTokenException") ||
                    className.contains("SpeechRecognizer") ||
                    className.contains("TextToSpeech") ||
                    throwable is android.os.TransactionTooLargeException ||
                    throwable is android.os.DeadObjectException ||
                    throwable is SecurityException && (msg.contains("MediaProjection") || msg.contains("FOREGROUND_SERVICE") || msg.contains("POST_NOTIFICATIONS"))
            if (!isRecoverableGlitch) {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        System.gc()
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_MODERATE) {
            System.gc()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_FLOATING_SERVICE,
                "Floating Assistant Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Silent internal channel for background overlay service"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_FLOATING_SERVICE = "channel_floating_ai_service"
        lateinit var instance: OmniAIApplication
            private set
    }
}
