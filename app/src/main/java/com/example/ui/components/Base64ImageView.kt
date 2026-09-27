package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val imageMemoryCache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int {
        return value.byteCount
    }
}

@Composable
fun Base64ImageView(
    base64String: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = "Message attachment",
    fillWidth: Boolean = true
) {
    var isViewerOpen by remember { mutableStateOf(false) }
    val isFilePath = remember(base64String) {
        base64String.isNotBlank() && (base64String.startsWith("/") || base64String.startsWith("file://"))
    }

    if (isFilePath) {
        val cleanPath = remember(base64String) { base64String.replace("file://", "") }
        val imageFile = remember(cleanPath) { File(cleanPath) }

        Box(
            modifier = modifier
                .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            AsyncImage(
                model = imageFile,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .heightIn(max = 240.dp)
                    .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { isViewerOpen = true }
            )
        }

        // Fullscreen Lightbox / Popup Image Viewer
        if (isViewerOpen) {
            Popup(
                onDismissRequest = { isViewerOpen = false },
                properties = PopupProperties(
                    focusable = true,
                    excludeFromSystemGesture = true
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.95f))
                        .clickable { isViewerOpen = false },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageFile,
                        contentDescription = "Full Image View",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )

                    IconButton(
                        onClick = { isViewerOpen = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(24.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Viewer",
                            tint = Color.White
                        )
                    }
                }
            }
        }
        return
    }

    val bitmapState by produceState<Bitmap?>(initialValue = imageMemoryCache.get(base64String), key1 = base64String) {
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            try {
                val cached = imageMemoryCache.get(base64String)
                if (cached != null) return@withContext cached

                val cleanBase64 = if (base64String.contains(",")) {
                    base64String.substringAfter(",")
                } else {
                    base64String
                }
                val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, options)
                var sampleSize = 1
                while ((options.outWidth / sampleSize) > 800 || (options.outHeight / sampleSize) > 800) {
                    sampleSize *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val decoded = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, decodeOpts)

                if (decoded != null) {
                    imageMemoryCache.put(base64String, decoded)
                }
                decoded
            } catch (e: Exception) {
                null
            }
        }
    }

    val currentBmp = bitmapState

    if (currentBmp != null && !currentBmp.isRecycled) {
        Box(
            modifier = modifier
                .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Image(
                bitmap = currentBmp.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .heightIn(max = 240.dp)
                    .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { isViewerOpen = true }
            )
        }

        // Fullscreen Lightbox / Popup Image Viewer
        if (isViewerOpen) {
            Popup(
                onDismissRequest = { isViewerOpen = false },
                properties = PopupProperties(
                    focusable = true,
                    excludeFromSystemGesture = true
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.95f))
                        .clickable { isViewerOpen = false },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = currentBmp.asImageBitmap(),
                        contentDescription = "Full Image View",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )

                    IconButton(
                        onClick = { isViewerOpen = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(24.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Viewer",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    } else {
        // Fast lightweight placeholder while loading
        Box(
            modifier = modifier
                .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                .height(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }
    }
}
