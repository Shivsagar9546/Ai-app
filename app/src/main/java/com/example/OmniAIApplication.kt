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

    lateinit var firestoreSyncRepository: com.example.data.firebase.FirestoreSyncRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        initFirebaseSafely()

        database = AppDatabase.getDatabase(this)
        adminPreferences = AdminPreferencesRepository(this)
        firestoreSyncRepository = com.example.data.firebase.FirestoreSyncRepository(this)

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
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = NotificationChannel(
                    CHANNEL_FLOATING_SERVICE,
                    "Floating Assistant (Silent)",
                    NotificationManager.IMPORTANCE_MIN
                ).apply {
                    description = "Silent background channel for floating assistant"
                    setShowBadge(false)
                    enableLights(false)
                    enableVibration(false)
                    setSound(null, null)
                }
                notificationManager.createNotificationChannel(channel)
            } catch (e: Exception) {}
        }
    }

    private fun initFirebaseSafely() {
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                val initialized = try {
                    com.google.firebase.FirebaseApp.initializeApp(this) != null
                } catch (e: Exception) {
                    false
                }
                if (!initialized && com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                    val options = com.google.firebase.FirebaseOptions.Builder()
                        .setApplicationId("1:667106647855:android:2245f2a5b78747b88ccbda")
                        .setApiKey("AIzaSyBptUTFKvOrCS0OfagKpzOfYWt2YVS8i5E")
                        .setProjectId("ai-app-a0551")
                        .setDatabaseUrl("https://ai-app-a0551-default-rtdb.asia-southeast1.firebasedatabase.app")
                        .setStorageBucket("ai-app-a0551.firebasestorage.app")
                        .build()
                    com.google.firebase.FirebaseApp.initializeApp(this, options)
                    android.util.Log.i("OmniAI", "FirebaseApp initialized with explicit fallback options")
                } else {
                    android.util.Log.i("OmniAI", "FirebaseApp auto-initialized successfully")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("OmniAI", "FirebaseApp safe init error", e)
        }
    }

    companion object {
        const val CHANNEL_FLOATING_SERVICE = "channel_floating_ai_service_min"
        lateinit var instance: OmniAIApplication
            private set
    }
}
