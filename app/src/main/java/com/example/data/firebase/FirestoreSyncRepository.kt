package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.ChatMessage
import com.example.data.local.Conversation
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class CloudSyncStatus {
    IDLE,
    SYNCING,
    SYNCED,
    ERROR
}

data class CloudSyncState(
    val status: CloudSyncStatus = CloudSyncStatus.IDLE,
    val lastSyncTime: Long? = null,
    val message: String? = null,
    val displayName: String = "Personal Cloud Storage (Private)",
    val isPermissionDenied: Boolean = false
)

class FirestoreSyncRepository(private val context: Context) {

    companion object {
        private const val PERSONAL_COLLECTION = "personal_backup"
        private const val PERSONAL_DOC = "owner_data"
        private const val TAG = "FirestoreSync"

        fun ensureFirebaseInitialized(context: Context) {
            try {
                if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                    val app = try {
                        com.google.firebase.FirebaseApp.initializeApp(context)
                    } catch (_: Exception) { null }

                    if (app == null && com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                        val options = com.google.firebase.FirebaseOptions.Builder()
                            .setApplicationId("1:667106647855:android:2245f2a5b78747b88ccbda")
                            .setApiKey("AIzaSyBptUTFKvOrCS0OfagKpzOfYWt2YVS8i5E")
                            .setProjectId("ai-app-a0551")
                            .setDatabaseUrl("https://ai-app-a0551-default-rtdb.asia-southeast1.firebasedatabase.app")
                            .setStorageBucket("ai-app-a0551.firebasestorage.app")
                            .build()
                        com.google.firebase.FirebaseApp.initializeApp(context, options)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "ensureFirebaseInitialized error: ${e.message}")
            }
        }
    }

    private val db: FirebaseFirestore by lazy {
        ensureFirebaseInitialized(context)
        val dbId = try {
            val res = context.getString(R.string.firestore_database_id)
            if (res.isBlank() || res == "(default)") null else res
        } catch (_: Exception) {
            null
        }
        val app = try {
            com.google.firebase.FirebaseApp.getInstance()
        } catch (_: Exception) {
            ensureFirebaseInitialized(context)
            com.google.firebase.FirebaseApp.getInstance()
        }
        if (dbId != null) {
            FirebaseFirestore.getInstance(app, dbId)
        } else {
            FirebaseFirestore.getInstance(app)
        }
    }

    private val _syncState = MutableStateFlow(
        CloudSyncState(
            status = CloudSyncStatus.IDLE,
            message = "Personal Cloud Database Active (ai-app-a0551)"
        )
    )
    val syncState: StateFlow<CloudSyncState> = _syncState.asStateFlow()

    private fun isPermissionDenied(e: Exception): Boolean {
        return (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) ||
                (e.cause is FirebaseFirestoreException && (e.cause as FirebaseFirestoreException).code == FirebaseFirestoreException.Code.PERMISSION_DENIED) ||
                (e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true)
    }

    private fun getMessageDocId(msg: ChatMessage): String {
        return if (msg.id > 0) msg.id.toString() else "${msg.timestamp}_${msg.role}"
    }

    private fun formatErrorMessage(e: Exception, operation: String): String {
        return if (isPermissionDenied(e)) {
            Log.w(TAG, "$operation: Firestore rules permission denied on project 'ai-app-a0551'.")
            "Firestore Permission Denied: Firebase Console (ai-app-a0551) me Rules me read/write allow karein."
        } else {
            Log.w(TAG, "$operation failed: ${e.message}")
            e.localizedMessage ?: "$operation failed"
        }
    }

    suspend fun backupConversationsToCloud(
        conversations: List<Conversation>,
        getMessagesForConv: suspend (String) -> List<ChatMessage>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            _syncState.value = _syncState.value.copy(
                status = CloudSyncStatus.SYNCING,
                message = "Uploading chats to your personal Cloud Firestore..."
            )
            var count = 0

            val ownerDoc = db.collection(PERSONAL_COLLECTION).document(PERSONAL_DOC)
            ownerDoc.set(
                mapOf(
                    "type" to "personal_private_vault",
                    "totalConversations" to conversations.size,
                    "lastBackupAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()

            for (conv in conversations) {
                val convRef = ownerDoc.collection("conversations").document(conv.id)
                convRef.set(
                    mapOf(
                        "id" to conv.id,
                        "title" to conv.title,
                        "createdAt" to conv.createdAt,
                        "updatedAt" to conv.updatedAt,
                        "lastMessagePreview" to conv.lastMessagePreview,
                        "modelUsed" to conv.modelUsed,
                        "syncedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()

                val messages = getMessagesForConv(conv.id)
                for (msg in messages) {
                    val msgRef = convRef.collection("messages").document(getMessageDocId(msg))
                    msgRef.set(
                        mapOf(
                            "id" to msg.id,
                            "conversationId" to msg.conversationId,
                            "role" to msg.role,
                            "text" to msg.text,
                            "isScreenScan" to msg.isScreenScan,
                            "timestamp" to msg.timestamp,
                            "isStreaming" to false,
                            "isError" to msg.isError
                        ),
                        SetOptions.merge()
                    ).await()
                }
                count++
            }

            _syncState.value = _syncState.value.copy(
                status = CloudSyncStatus.SYNCED,
                lastSyncTime = System.currentTimeMillis(),
                message = "Successfully backed up $count conversations to Personal Cloud Firestore",
                isPermissionDenied = false
            )
            Result.success(count)
        } catch (e: Exception) {
            val userMsg = formatErrorMessage(e, "Personal backup")
            val isPerm = isPermissionDenied(e)
            _syncState.value = _syncState.value.copy(
                status = CloudSyncStatus.ERROR,
                message = userMsg,
                isPermissionDenied = isPerm
            )
            Result.failure(Exception(userMsg, e))
        }
    }

    suspend fun restoreConversationsFromCloud(
        onRestoreConversation: suspend (Conversation, List<ChatMessage>) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            _syncState.value = _syncState.value.copy(
                status = CloudSyncStatus.SYNCING,
                message = "Restoring chats from your personal Cloud Firestore..."
            )
            val ownerDoc = db.collection(PERSONAL_COLLECTION).document(PERSONAL_DOC)
            val convSnapshot = ownerDoc.collection("conversations").get().await()

            var count = 0
            for (doc in convSnapshot.documents) {
                val convId = doc.getString("id") ?: doc.id
                val title = doc.getString("title") ?: "Chat"
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                val lastMessagePreview = doc.getString("lastMessagePreview") ?: ""
                val modelUsed = doc.getString("modelUsed") ?: "gemini-1.5-flash"

                val conv = Conversation(
                    id = convId,
                    title = title,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    lastMessagePreview = lastMessagePreview,
                    modelUsed = modelUsed
                )

                val msgSnapshot = doc.reference.collection("messages").get().await()
                val messages = msgSnapshot.documents.mapNotNull { mDoc ->
                    val mId = mDoc.getLong("id") ?: 0L
                    val role = mDoc.getString("role") ?: "assistant"
                    val text = mDoc.getString("text") ?: ""
                    val timestamp = mDoc.getLong("timestamp") ?: System.currentTimeMillis()
                    val isScreenScan = mDoc.getBoolean("isScreenScan") ?: false
                    val isError = mDoc.getBoolean("isError") ?: false
                    ChatMessage(
                        id = mId,
                        conversationId = convId,
                        role = role,
                        text = text,
                        isScreenScan = isScreenScan,
                        timestamp = timestamp,
                        isStreaming = false,
                        isError = isError
                    )
                }

                onRestoreConversation(conv, messages)
                count++
            }

            _syncState.value = _syncState.value.copy(
                status = CloudSyncStatus.SYNCED,
                lastSyncTime = System.currentTimeMillis(),
                message = "Successfully restored $count conversations from Personal Cloud Firestore",
                isPermissionDenied = false
            )
            Result.success(count)
        } catch (e: Exception) {
            val userMsg = formatErrorMessage(e, "Personal restore")
            val isPerm = isPermissionDenied(e)
            _syncState.value = _syncState.value.copy(
                status = CloudSyncStatus.ERROR,
                message = userMsg,
                isPermissionDenied = isPerm
            )
            Result.failure(Exception(userMsg, e))
        }
    }

    suspend fun deleteConversationFromCloud(convId: String) = withContext(Dispatchers.IO) {
        try {
            val ownerDoc = db.collection(PERSONAL_COLLECTION).document(PERSONAL_DOC)
            val convRef = ownerDoc.collection("conversations").document(convId)
            val msgSnapshot = convRef.collection("messages").get().await()
            val chunks = msgSnapshot.documents.chunked(400)
            for (chunk in chunks) {
                val batch = db.batch()
                for (mDoc in chunk) {
                    batch.delete(mDoc.reference)
                }
                batch.commit().await()
            }
            convRef.delete().await()
            Log.d(TAG, "Deleted conversation $convId from Firestore safely")
        } catch (e: Exception) {
            if (isPermissionDenied(e)) {
                Log.w(TAG, "deleteConversationFromCloud skipped: permission denied on remote project")
            } else {
                Log.w(TAG, "Error deleting conversation from cloud: $convId: ${e.message}")
            }
        }
    }

    suspend fun clearAllCloudBackup() = withContext(Dispatchers.IO) {
        try {
            val ownerDoc = db.collection(PERSONAL_COLLECTION).document(PERSONAL_DOC)
            val convSnapshot = ownerDoc.collection("conversations").get().await()
            for (doc in convSnapshot.documents) {
                val msgSnapshot = doc.reference.collection("messages").get().await()
                val chunks = msgSnapshot.documents.chunked(400)
                for (chunk in chunks) {
                    val batch = db.batch()
                    for (mDoc in chunk) {
                        batch.delete(mDoc.reference)
                    }
                    batch.commit().await()
                }
                doc.reference.delete().await()
            }
            ownerDoc.delete().await()
            _syncState.value = _syncState.value.copy(
                status = CloudSyncStatus.IDLE,
                message = "Cloud history cleared completely"
            )
            Log.d(TAG, "Cleared all conversations from Firestore")
        } catch (e: Exception) {
            if (isPermissionDenied(e)) {
                Log.w(TAG, "clearAllCloudBackup skipped: permission denied on remote project")
            } else {
                Log.w(TAG, "Error clearing cloud backup: ${e.message}")
            }
        }
    }

    /**
     * Micro Delta Batch Sync:
     * Commits dirty changes in an efficient batch in the background without blocking the UI.
     */
    suspend fun syncDeltaBatch(conv: Conversation, messages: List<ChatMessage>) = withContext(Dispatchers.IO) {
        try {
            val deltaMessages = if (messages.size > 100) messages.takeLast(100) else messages
            val batch = db.batch()
            val ownerDoc = db.collection(PERSONAL_COLLECTION).document(PERSONAL_DOC)
            batch.set(
                ownerDoc,
                mapOf(
                    "type" to "personal_private_vault",
                    "lastActiveAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )

            val convRef = ownerDoc.collection("conversations").document(conv.id)
            batch.set(
                convRef,
                mapOf(
                    "id" to conv.id,
                    "title" to conv.title,
                    "createdAt" to conv.createdAt,
                    "updatedAt" to conv.updatedAt,
                    "lastMessagePreview" to conv.lastMessagePreview,
                    "modelUsed" to conv.modelUsed,
                    "syncedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )

            for (msg in deltaMessages) {
                val msgRef = convRef.collection("messages").document(getMessageDocId(msg))
                batch.set(
                    msgRef,
                    mapOf(
                        "id" to msg.id,
                        "conversationId" to conv.id,
                        "role" to msg.role,
                        "text" to msg.text,
                        "isScreenScan" to msg.isScreenScan,
                        "timestamp" to msg.timestamp,
                        "isStreaming" to false,
                        "isError" to msg.isError
                    ),
                    SetOptions.merge()
                )
            }
            batch.commit().await()
            Log.d(TAG, "Micro delta batch synced successfully for conv ${conv.id} (${deltaMessages.size} messages)")
        } catch (e: Exception) {
            if (isPermissionDenied(e)) {
                Log.w(TAG, "Micro delta batch sync skipped: Firestore rules permission denied on project ai-app-a0551")
            } else {
                Log.w(TAG, "Micro delta batch sync error: ${e.message}")
            }
        }
    }

    suspend fun saveConversationDirectly(conv: Conversation) = withContext(Dispatchers.IO) {
        try {
            val ownerDoc = db.collection(PERSONAL_COLLECTION).document(PERSONAL_DOC)
            ownerDoc.set(
                mapOf(
                    "type" to "personal_private_vault",
                    "lastActiveAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()

            ownerDoc.collection("conversations").document(conv.id).set(
                mapOf(
                    "id" to conv.id,
                    "title" to conv.title,
                    "createdAt" to conv.createdAt,
                    "updatedAt" to conv.updatedAt,
                    "lastMessagePreview" to conv.lastMessagePreview,
                    "modelUsed" to conv.modelUsed,
                    "syncedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Log.w(TAG, "saveConversationDirectly skipped: permission denied on project ai-app-a0551")
            } else {
                Log.w(TAG, "saveConversationDirectly error: ${e.message}")
            }
        }
    }

    suspend fun saveMessageDirectly(convId: String, msg: ChatMessage) = withContext(Dispatchers.IO) {
        try {
            val ownerDoc = db.collection(PERSONAL_COLLECTION).document(PERSONAL_DOC)
            val convRef = ownerDoc.collection("conversations").document(convId)
            val msgRef = convRef.collection("messages").document(msg.id.toString())
            msgRef.set(
                mapOf(
                    "id" to msg.id,
                    "conversationId" to convId,
                    "role" to msg.role,
                    "text" to msg.text,
                    "isScreenScan" to msg.isScreenScan,
                    "timestamp" to msg.timestamp,
                    "isStreaming" to false,
                    "isError" to msg.isError
                ),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Log.w(TAG, "saveMessageDirectly skipped: permission denied on project ai-app-a0551")
            } else {
                Log.w(TAG, "saveMessageDirectly error: ${e.message}")
            }
        }
    }
}
