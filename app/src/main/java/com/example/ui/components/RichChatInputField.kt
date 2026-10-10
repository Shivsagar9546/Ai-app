package com.example.ui.components

import android.content.ClipData
import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
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

/**
 * Custom EditText that advertises full rich-content image media capability to Android IMEs
 * (such as Samsung Keyboard, Gboard, SwiftKey).
 *
 * When focused, the keyboard inspects EditorInfo.contentMimeTypes. Because image media is declared,
 * keyboards (like Samsung Keyboard in WhatsApp) display the recent screenshot / copied image
 * "Paste" suggestion chip directly inside the keyboard toolbar / suggestion strip!
 */
class RichContentEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : EditText(context, attrs, defStyleAttr) {

    private val tag = "RichContentEditText"

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
            // In payload.partition(predicate), split.first contains items where predicate is TRUE (i.e. has URI),
            // and split.second contains items where predicate is FALSE (e.g. plain text).
            val split = payload.partition { item -> item.uri != null }
            val imageContent = split.first
            val remaining = split.second

            if (imageContent != null) {
                Log.d(tag, "Image received via ViewCompat OnReceiveContentListener: ${imageContent.clip.itemCount} items")
                val clipData = imageContent.clip
                onImageReceived?.invoke(clipData)
            }
            remaining
        }
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val ic = super.onCreateInputConnection(outAttrs) ?: return null

        // Crucial: Advertise image MIME types to Samsung Keyboard, Gboard, SwiftKey, etc.
        // This is what triggers the screenshot / clipboard image "Paste" suggestion chip in the keyboard!
        EditorInfoCompat.setContentMimeTypes(outAttrs, supportedMimeTypes)

        // Wrapper for IME Commit Content API that connects to ViewCompat.setOnReceiveContentListener
        return InputConnectionCompat.createWrapper(this, ic, outAttrs)
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
                editText.onImageReceived = onImageReceived
                editText.onSendAction = onSend
                editText.setTextColor(textColor)
                editText.setHintTextColor(hintColor)
                editText.highlightColor = (primaryColor and 0x00FFFFFF) or 0x40000000

                // Sync text if external update occurred (e.g. cleared on send or voice input recognized)
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
