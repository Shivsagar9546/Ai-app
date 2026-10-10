package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import com.example.util.receiveImageContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.content.Intent
import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import android.net.Uri
import android.provider.MediaStore
import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChatMessage
import com.example.ui.components.AnimatedTypingIndicator
import com.example.ui.components.GeminiDirectWebView
import com.example.ui.components.MarkdownText
import com.example.ui.components.QuickActionChips
import com.example.ui.theme.AiBubbleGradientEnd
import com.example.ui.theme.AiBubbleGradientStart

@Composable
fun FloatingPopUpView(
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    statusText: String?,
    onSendMessage: (String, Bitmap?, List<Bitmap>) -> Unit,
    onInstantTextScan: () -> Unit = {},
    onScanScreen: () -> Unit = {},
    onAreaScan: () -> Unit,
    onScreenshotCapture: () -> Unit,
    onOcrGrabber: () -> Unit = {},
    onQuickHud: () -> Unit = {},
    onPickGalleryImage: () -> Unit = {},
    onSpeakText: (String) -> Unit = {},
    onClearMessages: () -> Unit = {},
    onRegenerate: () -> Unit = {},
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onClose: () -> Unit,
    onDragHeader: (dx: Float, dy: Float) -> Unit,
    onResize: (dw: Float, dh: Float, dx: Float, dy: Float) -> Unit,
    onAlphaChanged: (Float) -> Unit = {},
    externalAttachedBitmap: Bitmap? = null,
    onClearExternalAttachedBitmap: () -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    var attachedBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var currentlySpeakingText by remember { mutableStateOf<String?>(null) }
    var windowAlpha by remember { mutableStateOf(1.0f) }
    var isTransparencySliderOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(externalAttachedBitmap) {
        if (externalAttachedBitmap != null) {
            attachedBitmaps = (attachedBitmaps + externalAttachedBitmap).take(10)
            onClearExternalAttachedBitmap()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = windowAlpha.coerceIn(0.25f, 1.0f)),
        tonalElevation = 8.dp,
        shadowElevation = (24 * windowAlpha).dp,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = windowAlpha
            }
            .shadow((20 * windowAlpha).dp, RoundedCornerShape(22.dp))
            .border(
                1.5.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = (0.35f * windowAlpha).coerceAtLeast(0.15f)),
                RoundedCornerShape(22.dp)
            )
            .testTag("floating_popup_window")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Drag Handle & Controls Bar (Samsung Pop-up Style)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onDragHeader(dragAmount.x, dragAmount.y)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Title & Drag Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        listOf(AiBubbleGradientStart, AiBubbleGradientEnd)
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = "OmniAI",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // The two lines (=) icon requested by user to toggle transparency controls
                        Surface(
                            onClick = {
                                isTransparencySliderOpen = !isTransparencySliderOpen
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isTransparencySliderOpen) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .testTag("popup_transparency_toggle_button")
                                .padding(horizontal = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Adjust Transparency",
                                    tint = if (isTransparencySliderOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (windowAlpha < 0.98f) {
                                    Text(
                                        text = "${(windowAlpha * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (isTransparencySliderOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Window Control Actions: Clear Chat | Minimize | Maximize | Close
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Clear Chat History
                        if (messages.isNotEmpty()) {
                            IconButton(
                                onClick = onClearMessages,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("popup_clear_chat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear Chat",
                                    modifier = Modifier.size(17.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Minimize to Bubble
                        IconButton(
                            onClick = onMinimize,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("popup_minimize_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Minimize to bubble",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Maximize to Full Screen App
                        IconButton(
                            onClick = onMaximize,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("popup_maximize_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInFull,
                                contentDescription = "Open Full Screen",
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Close Popup
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("popup_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Interactive Transparency Slider Panel (Toggled via the = two lines icon)
                AnimatedVisibility(visible = isTransparencySliderOpen) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                            .testTag("popup_transparency_panel")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Window Transparency",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Slider(
                                value = windowAlpha,
                                onValueChange = { 
                                    windowAlpha = it
                                    onAlphaChanged(it)
                                },
                                valueRange = 0.25f..1.0f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("popup_transparency_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }

                // Smart AI Tools & Chat View
                // Status Banner (if capturing / analyzing screen)
                AnimatedVisibility(visible = statusText != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = statusText ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Chat Messages List
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                    ) {
                        if (messages.isEmpty() && !isGenerating) {
                            // Empty State / Quick Starter
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "How can I help you?",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Crop any screen area, extract text instantly, or ask questions below.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp)
                            ) {
                                items(messages, key = { "${it.id}_${it.role}_${it.timestamp}" }) { msg ->
                                    FloatingMessageBubble(
                                        msg = msg,
                                        isSpeaking = (currentlySpeakingText == msg.text),
                                        onSpeak = { text ->
                                            if (currentlySpeakingText == text) {
                                                currentlySpeakingText = null
                                                onSpeakText("")
                                            } else {
                                                currentlySpeakingText = text
                                                onSpeakText(text)
                                            }
                                        },
                                        onCopy = { text ->
                                            clipboardManager.setText(AnnotatedString(text))
                                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                        },
                                        onShare = { text ->
                                            try {
                                                val sendIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, text)
                                                    type = "text/plain"
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                val shareIntent = Intent.createChooser(sendIntent, "Share AI Response").apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(shareIntent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Unable to share", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onRegenerate = onRegenerate
                                    )
                                }

                                if (isGenerating) {
                                    item {
                                        AnimatedTypingIndicator(
                                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Quick Action Chips (Explain, Solve, Translate, etc.) - Only visible when empty
                    if (messages.isEmpty() && attachedBitmaps.isEmpty()) {
                        QuickActionChips(
                            onActionSelected = { prompt ->
                                onSendMessage(prompt, null, attachedBitmaps)
                                attachedBitmaps = emptyList()
                            }
                        )
                    }

                    // Attached Images Preview Bar (if user picked any images)
                    AnimatedVisibility(visible = attachedBitmaps.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Attached Photos (${attachedBitmaps.size}/10)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Clear All",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.clickable { attachedBitmaps = emptyList() }
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    itemsIndexed(attachedBitmaps) { index, bmp ->
                                        if (!bmp.isRecycled) {
                                            Box(
                                                modifier = Modifier.size(56.dp)
                                            ) {
                                                Image(
                                                    bitmap = bmp.asImageBitmap(),
                                                    contentDescription = "Attached photo preview",
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .border(
                                                            1.dp,
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                            RoundedCornerShape(8.dp)
                                                        ),
                                                    contentScale = ContentScale.Crop
                                                )
                                                IconButton(
                                                    onClick = {
                                                        val mutable = attachedBitmaps.toMutableList()
                                                        mutable.removeAt(index)
                                                        attachedBitmaps = mutable
                                                    },
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .size(16.dp)
                                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Remove photo",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Floating Control Bar
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            // Quick scan & vision tools scrollable bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 0. Instant Screen Text
                                Button(
                                    onClick = onInstantTextScan,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("popup_instant_text_button")
                                 ) {
                                     Icon(
                                         imageVector = Icons.Default.Bolt,
                                         contentDescription = "Instant Text",
                                         modifier = Modifier.size(13.dp)
                                     )
                                     Spacer(modifier = Modifier.width(3.dp))
                                     Text("⚡ Instant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                 }

                                // 0.1 Screenshot Capture
                                Button(
                                    onClick = onScreenshotCapture,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary,
                                        contentColor = MaterialTheme.colorScheme.onTertiary
                                    ),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("popup_screenshot_button")
                                 ) {
                                     Icon(
                                         imageVector = Icons.Default.Screenshot,
                                         contentDescription = "Screenshot",
                                         modifier = Modifier.size(13.dp)
                                     )
                                     Spacer(modifier = Modifier.width(3.dp))
                                     Text("📷 Screen", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                 }

                                // 1. Crop Selected Area
                                Button(
                                    onClick = onAreaScan,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("popup_area_scan_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CropFree,
                                        contentDescription = "Crop Area",
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("✂️ Crop", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                // 2. Gallery Photo Upload
                                Button(
                                    onClick = onPickGalleryImage,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("popup_gallery_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = "Gallery",
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("🖼️ Gallery", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                // 3. Clipboard Screenshot Paste Button
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val bitmaps = com.example.util.ClipboardImageHelper.getBitmapsFromClipboard(context)
                                            if (bitmaps.isNotEmpty()) {
                                                attachedBitmaps = (attachedBitmaps + bitmaps).take(10)
                                                Toast.makeText(context, "Pasted ${bitmaps.size} image(s) from clipboard", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "No screenshot/image in clipboard", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("popup_paste_clipboard_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste Clipboard Screenshot",
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("📋 Paste", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Input Field with Attach, Voice & Send Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Direct Image Attachment (+) / Photo Button from Gallery
                                IconButton(
                                    onClick = onPickGalleryImage,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                        .testTag("popup_attach_image_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = "Upload Image from Gallery",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                OutlinedTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    placeholder = {
                                        Text(
                                            text = if (attachedBitmaps.isNotEmpty()) "Ask about these photos..." else "Ask about screen or type...",
                                            fontSize = 13.sp
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .receiveImageContent { clipData ->
                                            coroutineScope.launch {
                                                val bitmaps = com.example.util.ClipboardImageHelper.extractBitmapsFromClipData(context, clipData)
                                                if (bitmaps.isNotEmpty()) {
                                                    attachedBitmaps = (attachedBitmaps + bitmaps).take(10)
                                                    Toast.makeText(context, "Pasted ${bitmaps.size} image(s)", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                        .testTag("popup_input_field"),
                                    shape = RoundedCornerShape(20.dp),
                                    singleLine = false,
                                    maxLines = 3,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                )



                                // Send Button
                                FilledIconButton(
                                    onClick = {
                                        val prompt = inputText.trim()
                                        if (prompt.isNotBlank() || attachedBitmaps.isNotEmpty()) {
                                            onSendMessage(prompt, null, attachedBitmaps)
                                            inputText = ""
                                            attachedBitmaps = emptyList()
                                        }
                                    },
                                    enabled = (inputText.isNotBlank() || attachedBitmaps.isNotEmpty()) && !isGenerating,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("popup_send_button"),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

            // Bottom-Right Corner Resize Grip Handle (Non-intrusive, leaves top buttons completely unblocked)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp)
                    .testTag("popup_resize_handle")
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onResize(dragAmount.x, dragAmount.y, dragAmount.x / 2f, dragAmount.y / 2f)
                        }
                    },
                contentAlignment = Alignment.BottomEnd
            ) {
                // Subtle diagonal grip icon for visual cue
                Canvas(
                    modifier = Modifier
                        .size(16.dp)
                        .padding(bottom = 4.dp, end = 4.dp)
                ) {
                    val strokeW = 2.dp.toPx()
                    val primaryColor = androidx.compose.ui.graphics.Color(0xFF8B5CF6)
                    drawLine(
                        color = primaryColor.copy(alpha = 0.6f),
                        start = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.4f),
                        end = androidx.compose.ui.geometry.Offset(size.width * 0.4f, size.height),
                        strokeWidth = strokeW
                    )
                    drawLine(
                        color = primaryColor.copy(alpha = 0.8f),
                        start = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.75f),
                        end = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height),
                        strokeWidth = strokeW
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingMessageBubble(
    msg: ChatMessage,
    isSpeaking: Boolean,
    onSpeak: (String) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onRegenerate: () -> Unit
) {
    val isUser = msg.role.equals("user", ignoreCase = true)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = when {
                msg.isError -> MaterialTheme.colorScheme.errorContainer
                isUser -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.95f)
        ) {
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                if (msg.isScreenScan) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CropFree,
                            contentDescription = "Screen Scan",
                            modifier = Modifier.size(13.dp),
                            tint = if (isUser) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Screen Query",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (!msg.imageBase64.isNullOrBlank()) {
                    val imagesList = msg.imageBase64.split("|").filter { it.isNotBlank() }
                    if (imagesList.size > 1) {
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            items(imagesList) { imgPath ->
                                com.example.ui.components.Base64ImageView(
                                    base64String = imgPath,
                                    contentDescription = "Uploaded photo preview",
                                    modifier = Modifier.size(90.dp).clip(RoundedCornerShape(6.dp)),
                                    fillWidth = false
                                )
                            }
                        }
                    } else if (imagesList.isNotEmpty()) {
                        com.example.ui.components.Base64ImageView(
                            base64String = imagesList[0],
                            contentDescription = "Screen capture preview"
                        )
                    }
                }

                if (isUser) {
                    if (msg.text.isNotBlank()) {
                        Text(
                            text = msg.text,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    MarkdownText(
                        text = msg.text,
                        textColor = if (msg.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Action bar for AI response (Speak, Share, Copy, Regenerate)
                    if (!msg.isError && msg.text.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onSpeak(msg.text) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = if (isSpeaking) "Stop voice" else "Read aloud",
                                    tint = if (isSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = { onCopy(msg.text) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy text",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            IconButton(
                                onClick = { onShare(msg.text) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share text",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            IconButton(
                                onClick = onRegenerate,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Regenerate answer",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
