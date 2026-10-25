package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.ChatMessage
import com.example.data.local.Conversation
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
    val displayName: String = "Personal Cloud Storage (Private)"
)

class FirestoreSyncRepository(private val context: Context) {

    companion object {
        private const val PERSONAL_COLLECTION = "personal_backup"
        private const val PERSONAL_DOC = "owner_data"
    }

    private val db: FirebaseFirestore by lazy {
        val dbId = try {
            val res = context.getString(R.string.firestore_database_id)
            if (res.isBlank() || res == "(default)") null else res
        } catch (_: Exception) {
            null
        }
        if (dbId != null) {
            FirebaseFirestore.getInstance(dbId)
        } else {
            FirebaseFirestore.getInstance()
        }
    }

    private val _syncState = MutableStateFlow(
        CloudSyncState(
            status = CloudSyncStatus.IDLE,
            message = "Personal Cloud Database Active (ai-app-a0551)"
        )
    )
    val syncState: StateFlow<CloudSyncState> = _syncState.asStateFlow()

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
                    val msgRef = convRef.collection("messages").document(msg.id.toString())
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
                message = "Successfully backed up $count conversations to Personal Cloud Firestore"
            )
            Result.success(count)
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Personal backup error", e)
            _syncState.value = _syncState.value.copy(
                status = CloudSyncStatus.ERROR,
                message = e.localizedMessage ?: "Backup failed"
            )
            Result.failure(e)
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
                message = "Successfully restored $count conversations from Personal Cloud Firestore"
            )
            Result.success(count)
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Personal restore error", e)
            _syncState.value = _syncState.value.copy(
                status = CloudSyncStatus.ERROR,
                message = e.localizedMessage ?: "Restore failed"
            )
            Result.failure(e)
        }
    }
}
