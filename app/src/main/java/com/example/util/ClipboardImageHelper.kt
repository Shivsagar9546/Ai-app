package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.MediaType
import androidx.compose.foundation.content.ReceiveContentListener
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.consume
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.content.hasMediaType
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.receiveImageContent(
    onClipDataReceived: (ClipData) -> Unit
): Modifier = this.contentReceiver(
    object : ReceiveContentListener {
        override fun onReceive(transferableContent: TransferableContent): TransferableContent? {
            if (transferableContent.hasMediaType(MediaType.Image)) {
                val clipData = transferableContent.clipEntry.clipData
                onClipDataReceived(clipData)
                return transferableContent.consume { it.uri != null }
            }
            return transferableContent
        }
    }
)

object ClipboardImageHelper {

    private const val TAG = "ClipboardImageHelper"

    /**
     * Checks if the system clipboard currently contains an image or screenshot URI,
     * or if a screenshot was recently captured on device.
     */
    fun hasImageInClipboard(context: Context): Boolean {
        // 1. Check direct ClipboardManager
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val description = clip.description
                    if (description != null) {
                        if (description.hasMimeType("image/*") || 
                            description.hasMimeType("image/png") || 
                            description.hasMimeType("image/jpeg") ||
                            description.hasMimeType("image/webp") ||
                            description.hasMimeType("image/heic")
                        ) {
                            return true
                        }
                    }
                    val item = clip.getItemAt(0)
                    val uri = item?.uri
                    if (uri != null) {
                        val type = try { context.contentResolver.getType(uri) } catch (_: Exception) { null }
                        if (type?.startsWith("image/") == true) {
                            return true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking clipboard for image: ${e.message}")
        }

        // 2. Check for recent screenshot taken in the last 3 minutes
        return try {
            getRecentScreenshotUri(context) != null
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Looks up the latest screenshot taken on this device in the last 3 minutes.
     */
    fun getRecentScreenshotUri(context: Context): Uri? {
        return try {
            val threeMinutesAgo = (System.currentTimeMillis() / 1000) - 180
            val projection = arrayOf(
                android.provider.MediaStore.Images.Media._ID,
                android.provider.MediaStore.Images.Media.DATE_ADDED,
                android.provider.MediaStore.Images.Media.DISPLAY_NAME
            )
            val selection = "${android.provider.MediaStore.Images.Media.DATE_ADDED} >= ?"
            val selectionArgs = arrayOf(threeMinutesAgo.toString())
            val sortOrder = "${android.provider.MediaStore.Images.Media.DATE_ADDED} DESC"

            context.contentResolver.query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media._ID)
                    val nameCol = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.DISPLAY_NAME)
                    val name = if (nameCol != -1) cursor.getString(nameCol)?.lowercase() ?: "" else ""
                    val id = cursor.getLong(idCol)
                    
                    // Prioritize if filename contains screenshot / capture
                    if (name.contains("screenshot") || name.contains("screen") || name.contains("capture") || name.contains("img_")) {
                        return@use android.content.ContentUris.withAppendedId(
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                    }
                    // Fallback to recent image
                    return@use android.content.ContentUris.withAppendedId(
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                }
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extracts all images from the provided ClipData as sampled Bitmaps.
     */
    suspend fun extractBitmapsFromClipData(
        context: Context,
        clipData: ClipData?,
        maxDim: Int = 1024
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        if (clipData == null || clipData.itemCount == 0) return@withContext bitmaps

        for (i in 0 until clipData.itemCount) {
            val item = clipData.getItemAt(i) ?: continue

            // 1. Check URI
            val uri = item.uri
            if (uri != null) {
                decodeSampledBitmapFromUri(context, uri, maxDim, maxDim)?.let { bmp ->
                    bitmaps.add(bmp)
                    return@let
                }
            }

            // 2. Check Intent extra if present
            item.intent?.let { intent ->
                val dataUri = intent.data
                if (dataUri != null) {
                    decodeSampledBitmapFromUri(context, dataUri, maxDim, maxDim)?.let { bmp ->
                        bitmaps.add(bmp)
                    }
                }
            }
        }
        bitmaps
    }

    /**
     * Extracts images directly from the system clipboard or recent screenshot.
     */
    suspend fun getBitmapsFromClipboard(
        context: Context,
        maxDim: Int = 1024
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        // 1. Try system clipboard first
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val extracted = extractBitmapsFromClipData(context, clip, maxDim)
                    if (extracted.isNotEmpty()) {
                        return@withContext extracted
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading image from clipboard: ${e.message}")
        }

        // 2. Try recent screenshot if clipboard had no image
        try {
            val recentScreenshotUri = getRecentScreenshotUri(context)
            if (recentScreenshotUri != null) {
                decodeSampledBitmapFromUri(context, recentScreenshotUri, maxDim, maxDim)?.let { bmp ->
                    return@withContext listOf(bmp)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading recent screenshot: ${e.message}")
        }

        emptyList()
    }

    /**
     * Decodes a sampled bitmap from a content/file URI safely without OOM.
     */
    private fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return null
            }

            var inSampleSize = 1
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode URI $uri: ${e.message}")
            null
        }
    }
}
