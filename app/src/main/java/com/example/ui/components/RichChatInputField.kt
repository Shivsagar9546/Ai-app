package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat

/**
 * Custom EditText that advertises full rich-content image media capability to Android IMEs
 * (such as Samsung Keyboard, Gboard, SwiftKey).
 *
 * It intercepts:
 * 1. IME CommitContent (image paste suggestions in keyboard ribbon)
 * 2. System Clipboard / Long-press Paste (onTextContextMenuItem)
 * 3. Unified Content Receiver (Android 12+)
 *
 * It immediately requests URI permissions and decodes the Bitmap synchronously or safely
 * to avoid permission expiration, delivering ready-to-use Bitmaps directly to UI states.
 */
class RichContentEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : EditText(context, attrs, defStyleAttr) {

    private val tag = "RichContentEditText"

    var onBitmapReceived: ((Bitmap) -> Unit)? = null
    var onImageReceived: ((ClipData) -> Unit)? = null
    var onSendAction: (() -> Unit)? = null

    private val supportedMimeTypes = arrayOf(
        "image/png",
        "image/jpeg",
        "image/jpg",
        "image/webp",
        "image/gif",
        "image/heic",
        "image/*"
    )

    init {
        // Register Android 12+ / Jetpack unified content receiver for rich media paste & drag-and-drop
        ViewCompat.setOnReceiveContentListener(this, supportedMimeTypes) { _, payload ->
            val split = payload.partition { item -> item.uri != null }
            val imageContent = split.first
            val remaining = split.second

            if (imageContent != null) {
                Log.d(tag, "Image received via ViewCompat OnReceiveContentListener: ${imageContent.clip.itemCount} items")
                val clipData = imageContent.clip
                decodeAndDeliverClipData(clipData)
            }
            remaining
        }
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val ic = super.onCreateInputConnection(outAttrs) ?: return null

        // Crucial: Advertise image MIME types to Samsung Keyboard, Gboard, SwiftKey, etc.
        EditorInfoCompat.setContentMimeTypes(outAttrs, supportedMimeTypes)

        val callback = InputConnectionCompat.OnCommitContentListener { inputContentInfo: InputContentInfoCompat, flags: Int, _ ->
            val hasPermission = (flags and InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && hasPermission) {
                try {
                    inputContentInfo.requestPermission()
                } catch (e: Exception) {
                    Log.e(tag, "Failed to request URI permission: ${e.message}")
                    return@OnCommitContentListener false
                }
            }

            try {
                val uri = inputContentInfo.contentUri
                val decodedBmp = decodeUriSafely(context, uri)
                if (decodedBmp != null) {
                    post { onBitmapReceived?.invoke(decodedBmp) }
                    return@OnCommitContentListener true
                }

                // Fallback to ClipData
                val clipData = ClipData.newUri(context.contentResolver, "Pasted Image", uri)
                post { onImageReceived?.invoke(clipData) }
                true
            } catch (e: Exception) {
                Log.e(tag, "Error processing commitContent: ${e.message}")
                false
            } finally {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && hasPermission) {
                    try {
                        inputContentInfo.releasePermission()
                    } catch (_: Exception) {}
                }
            }
        }

        return InputConnectionCompat.createWrapper(ic, outAttrs, callback)
    }

    override fun onTextContextMenuItem(id: Int): Boolean {
        if (id == android.R.id.paste || id == android.R.id.pasteAsPlainText) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = clipboard?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                if (decodeAndDeliverClipData(clip)) {
                    return true
                }
            }
        }
        return super.onTextContextMenuItem(id)
    }

    private fun decodeAndDeliverClipData(clip: ClipData): Boolean {
        var handled = false
        for (i in 0 until clip.itemCount) {
            val item = clip.getItemAt(i) ?: continue

            // 1. Direct Content URI
            val uri = item.uri
            if (uri != null) {
                val bmp = decodeUriSafely(context, uri)
                if (bmp != null) {
                    onBitmapReceived?.invoke(bmp)
                    handled = true
                    continue
                }
            }

            // 2. Intent data URI
            item.intent?.data?.let { intentUri ->
                val bmp = decodeUriSafely(context, intentUri)
                if (bmp != null) {
                    onBitmapReceived?.invoke(bmp)
                    handled = true
                    return@let
                }
            }

            // 3. HTML text with embedded image (Samsung Keyboard clipboard)
            val html = item.htmlText ?: ""
            if (html.contains("<img", ignoreCase = true) && html.contains("src=", ignoreCase = true)) {
                val src = html.substringAfter("src=\"", "").substringBefore("\"", "")
                    .ifBlank { html.substringAfter("src=\'", "").substringBefore("\'", "") }
                if (src.isNotBlank()) {
                    if (src.startsWith("data:image", ignoreCase = true) && src.contains("base64,")) {
                        val base64Data = src.substringAfter("base64,")
                        val bmp = decodeBase64Safely(base64Data)
                        if (bmp != null) {
                            onBitmapReceived?.invoke(bmp)
                            handled = true
                            continue
                        }
                    } else if (src.startsWith("content://") || src.startsWith("file://")) {
                        val parsed = Uri.parse(src)
                        val bmp = decodeUriSafely(context, parsed)
                        if (bmp != null) {
                            onBitmapReceived?.invoke(bmp)
                            handled = true
                            continue
                        }
                    }
                }
            }

            // 4. Plain text containing image data or URI
            val textStr = item.text?.toString()?.trim() ?: ""
            if (textStr.startsWith("content://") || textStr.startsWith("file://")) {
                val parsed = Uri.parse(textStr)
                val bmp = decodeUriSafely(context, parsed)
                if (bmp != null) {
                    onBitmapReceived?.invoke(bmp)
                    handled = true
                    continue
                }
            } else if (textStr.startsWith("data:image", ignoreCase = true) && textStr.contains("base64,")) {
                val base64Data = textStr.substringAfter("base64,")
                val bmp = decodeBase64Safely(base64Data)
                if (bmp != null) {
                    onBitmapReceived?.invoke(bmp)
                    handled = true
                    continue
                }
            }
        }

        if (handled) return true

        // If not decoded directly into bitmap, pass full ClipData as fallback
        val hasPotentialImage = (0 until clip.itemCount).any { idx ->
            val itm = clip.getItemAt(idx)
            itm.uri != null || itm.intent?.data != null || (itm.htmlText?.contains("<img") == true)
        }
        if (hasPotentialImage) {
            onImageReceived?.invoke(clip)
            return true
        }
        return false
    }

    private fun decodeUriSafely(context: Context, uri: Uri): Bitmap? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return null
            if (bytes.isEmpty()) return null

            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)
            if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

            var inSampleSize = 1
            val maxDim = 1200
            val width = boundsOpts.outWidth
            val height = boundsOpts.outHeight
            while ((width / inSampleSize) > maxDim || (height / inSampleSize) > maxDim) {
                inSampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                this.inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
        } catch (e: Exception) {
            Log.e(tag, "decodeUriSafely failed for $uri: ${e.message}")
            null
        }
    }

    private fun decodeBase64Safely(base64Data: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Modern Jetpack Compose wrapper for RichContentEditText.
 * Provides a clean M3 rounded input pill with full native keyboard screenshot paste support.
 */
@Composable
fun RichChatInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onImageReceived: (ClipData) -> Unit,
    onBitmapReceived: ((Bitmap) -> Unit)? = null,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f).toArgb()
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 14.dp, vertical = 3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp, max = 130.dp),
            factory = { context ->
                RichContentEditText(context).apply {
                    this.hint = placeholder
                    this.setHintTextColor(hintColor)
                    this.setTextColor(textColor)
                    this.highlightColor = (primaryColor and 0x00FFFFFF) or 0x40000000
                    this.textSize = 15f
                    this.background = null // Remove default line
                    this.setPadding(6, 12, 6, 12)
                    this.gravity = Gravity.CENTER_VERTICAL or Gravity.START
                    this.maxLines = 5
                    this.isVerticalScrollBarEnabled = true

                    this.inputType = InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                            InputType.TYPE_TEXT_FLAG_MULTI_LINE

                    this.imeOptions = EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_ENTER_ACTION

                    this.onBitmapReceived = onBitmapReceived
                    this.onImageReceived = onImageReceived
                    this.onSendAction = onSend

                    this.setOnEditorActionListener { _, actionId, event ->
                        if (actionId == EditorInfo.IME_ACTION_SEND ||
                            (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && !event.isShiftPressed)
                        ) {
                            onSend()
                            true
                        } else {
                            false
                        }
                    }

                    this.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                            val newText = s?.toString() ?: ""
                            if (newText != value) {
                                onValueChange(newText)
                            }
                        }
                        override fun afterTextChanged(s: Editable?) {}
                    })
                }
            },
            update = { editText ->
                editText.onBitmapReceived = onBitmapReceived
                editText.onImageReceived = onImageReceived
                editText.onSendAction = onSend
                editText.setTextColor(textColor)
                editText.setHintTextColor(hintColor)
                editText.highlightColor = (primaryColor and 0x00FFFFFF) or 0x40000000

                // Sync text if external update occurred
                if (editText.text?.toString() != value) {
                    val selectionStart = editText.selectionStart
                    val selectionEnd = editText.selectionEnd
                    editText.setText(value)
                    if (value.isNotEmpty()) {
                        val safeStart = selectionStart.coerceIn(0, value.length)
                        val safeEnd = selectionEnd.coerceIn(0, value.length)
                        editText.setSelection(safeStart, safeEnd)
                    }
                }
            }
        )
    }
}
