package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.AiBubbleGradientEnd
import com.example.ui.theme.AiBubbleGradientStart
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FloatingBubbleView(
    bubbleStyle: String = "pill", // "pill", "circle", "icon_only", "square"
    customImagePath: String = "",
    presetIcon: String = "sparkle",
    customText: String = "AI ✨",
    gradientPreset: String = "purple",
    bubbleSize: String = "medium",
    bubbleAlpha: Float = 1.0f,
    onUpdateAlpha: ((Float) -> Unit)? = null,
    onDrag: (dx: Float, dy: Float) -> Unit = { _, _ -> },
    onDragEnd: () -> Unit = {},
    onBubbleClick: () -> Unit,
    onInstantTextScan: () -> Unit = {},
    onScreenshotCapture: () -> Unit = {},
    onScanScreen: () -> Unit,
    onAreaScan: () -> Unit,
    onOcrGrabber: () -> Unit,
    onQuickHud: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val gradientColors = remember(gradientPreset) {
        when (gradientPreset.lowercase()) {
            "cyan" -> listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
            "sunset" -> listOf(Color(0xFFFF512F), Color(0xFFDD2476))
            "emerald" -> listOf(Color(0xFF11998E), Color(0xFF38EF7D))
            "dark" -> listOf(Color(0xFF232526), Color(0xFF414345))
            "gold" -> listOf(Color(0xFFF7971E), Color(0xFFFFD200))
            else -> listOf(AiBubbleGradientStart, AiBubbleGradientEnd)
        }
    }

    val iconVector = remember(presetIcon) {
        when (presetIcon.lowercase()) {
            "robot" -> Icons.Default.SmartToy
            "brain" -> Icons.Default.Psychology
            "flash" -> Icons.Default.Bolt
            "target" -> Icons.Default.RadioButtonChecked
            "flame" -> Icons.Default.Whatshot
            "diamond" -> Icons.Default.Diamond
            "eye" -> Icons.Default.RemoveRedEye
            else -> Icons.Default.AutoAwesome
        }
    }

    val paddingH = when (bubbleSize) {
        "small" -> 10.dp
        "large" -> 18.dp
        else -> 14.dp
    }
    val paddingV = when (bubbleSize) {
        "small" -> 6.dp
        "large" -> 12.dp
        else -> 9.dp
    }
    val fontSize = when (bubbleSize) {
        "small" -> 12.sp
        "large" -> 15.sp
        else -> 13.5.sp
    }
    val iconSize = when (bubbleSize) {
        "small" -> 16.dp
        "large" -> 22.dp
        else -> 18.dp
    }
    val circleSize = when (bubbleSize) {
        "small" -> 42.dp
        "large" -> 58.dp
        else -> 48.dp
    }

    val hasCustomImage = remember(customImagePath) {
        customImagePath.isNotBlank() && File(customImagePath).exists()
    }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(8.dp)
    ) {
        // Quick Actions Menu on Long Press
        AnimatedVisibility(
            visible = showMenu,
            enter = fadeIn() + scaleIn(spring(stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut() + scaleOut()
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier
                    .width(200.dp)
                    .testTag("floating_bubble_menu")
            ) {
                Column(
                    modifier = Modifier.padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    BubbleMenuItem(
                        icon = Icons.Default.OpenInFull,
                        label = "Open Mini Window",
                        onClick = {
                            showMenu = false
                            onBubbleClick()
                        }
                    )
                    BubbleMenuItem(
                        icon = Icons.Default.Bolt,
                        label = "⚡ Instant Text (No Popup)",
                        onClick = {
                            showMenu = false
                            onInstantTextScan()
                        }
                    )
                    BubbleMenuItem(
                        icon = Icons.Default.Screenshot,
                        label = "📷 Instant Screenshot",
                        onClick = {
                            showMenu = false
                            onScreenshotCapture()
                        }
                    )
                    BubbleMenuItem(
                        icon = Icons.Default.CropFree,
                        label = "Crop Area",
                        onClick = {
                            showMenu = false
                            onAreaScan()
                        }
                    )
                    BubbleMenuItem(
                        icon = Icons.Default.TextFields,
                        label = "Text Grabber (OCR)",
                        onClick = {
                            showMenu = false
                            onOcrGrabber()
                        }
                    )
                    BubbleMenuItem(
                        icon = Icons.Default.AutoAwesome,
                        label = "Quick HUD Solution",
                        onClick = {
                            showMenu = false
                            onQuickHud()
                        }
                    )


                    // Quick Bubble Opacity / Transparency Row
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        val currentPct = (bubbleAlpha * 100).toInt()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Opacity,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text("Transparency", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text("$currentPct%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            listOf(
                                0.25f to "25%",
                                0.50f to "50%",
                                0.75f to "75%",
                                1.0f to "100%"
                            ).forEach { (alphaVal, label) ->
                                val isSelected = Math.abs(bubbleAlpha - alphaVal) < 0.12f
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { onUpdateAlpha?.invoke(alphaVal) }
                                        .padding(vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    BubbleMenuItem(
                        icon = Icons.Default.Settings,
                        label = "Settings & Custom Icon",
                        onClick = {
                            showMenu = false
                            onOpenSettings()
                        }
                    )
                    BubbleMenuItem(
                        icon = Icons.Default.Close,
                        label = "Close Bubble",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = {
                            showMenu = false
                            onClose()
                        }
                    )
                }
            }
        }

        // Common Gesture Modifier for smooth dragging across screen, short tap to open window, and long press for menu
        val bubbleGestureModifier = Modifier
            .testTag("floating_ai_bubble")
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startTime = System.currentTimeMillis()
                    var totalDragDistance = 0f
                    val touchSlop = viewConfiguration.touchSlop
                    var isDrag = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull()
                        if (change == null) {
                            if (isDrag) onDragEnd()
                            break
                        }

                        if (change.changedToUp()) {
                            change.consume()
                            if (isDrag) {
                                onDragEnd()
                            } else {
                                val duration = System.currentTimeMillis() - startTime
                                if (duration > 400) {
                                    showMenu = !showMenu
                                } else {
                                    if (showMenu) showMenu = false else onBubbleClick()
                                }
                            }
                            break
                        }

                        val dragX = change.position.x - change.previousPosition.x
                        val dragY = change.position.y - change.previousPosition.y
                        val dist = Math.hypot(dragX.toDouble(), dragY.toDouble()).toFloat()
                        totalDragDistance += dist

                        if (!isDrag && totalDragDistance > touchSlop) {
                            isDrag = true
                        }

                        if (isDrag) {
                            change.consume()
                            onDrag(dragX, dragY)
                        }
                    }
                }
            }

        // Main Draggable Custom Bubble
        Box(
            modifier = Modifier.alpha(bubbleAlpha.coerceIn(0.15f, 1.0f))
        ) {
            when {
                // Circle Avatar Style (with custom photo or icon)
            bubbleStyle == "circle" -> {
                Surface(
                    shape = CircleShape,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .size(circleSize)
                        .shadow(12.dp, CircleShape)
                        .clip(CircleShape)
                        .then(bubbleGestureModifier)
                ) {
                    Box(
                        modifier = Modifier
                            .size(circleSize)
                            .background(Brush.linearGradient(gradientColors))
                            .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasCustomImage) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(File(customImagePath))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Custom AI Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxDimensions()
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = "AI Assistant",
                                tint = Color.White,
                                modifier = Modifier.size(iconSize + 4.dp)
                            )
                        }
                    }
                }
            }

            // Icon Only (Compact Square / Rounded)
            bubbleStyle == "icon_only" || bubbleStyle == "square" -> {
                val shape = RoundedCornerShape(14.dp)
                Surface(
                    shape = shape,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .size(circleSize)
                        .shadow(12.dp, shape)
                        .clip(shape)
                        .then(bubbleGestureModifier)
                ) {
                    Box(
                        modifier = Modifier
                            .size(circleSize)
                            .background(Brush.linearGradient(gradientColors))
                            .border(1.5.dp, Color.White.copy(alpha = 0.5f), shape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasCustomImage) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(File(customImagePath))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Custom AI Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxDimensions()
                                    .clip(shape)
                            )
                        } else {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = "AI Assistant",
                                tint = Color.White,
                                modifier = Modifier.size(iconSize + 4.dp)
                            )
                        }
                    }
                }
            }

            // Default Pill Style (with Icon/Image + Text)
            else -> {
                val shape = RoundedCornerShape(26.dp)
                Surface(
                    shape = shape,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .shadow(12.dp, shape)
                        .clip(shape)
                        .then(bubbleGestureModifier)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Brush.linearGradient(gradientColors))
                            .border(1.5.dp, Color.White.copy(alpha = 0.45f), shape)
                            .padding(horizontal = paddingH, vertical = paddingV),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (hasCustomImage) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(File(customImagePath))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Custom AI Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(iconSize + 4.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, Color.White, CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = "AI Assistant",
                                    tint = Color.White,
                                    modifier = Modifier.size(iconSize)
                                )
                            }

                            val displayText = if (customText.isNotBlank()) customText else "AI ✨"
                            Text(
                                text = displayText,
                                color = Color.White,
                                fontSize = fontSize,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
}

private fun Modifier.fillMaxDimensions(): Modifier = this.then(
    Modifier
        .width(64.dp)
        .height(64.dp)
)

@Composable
private fun BubbleMenuItem(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .height(36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 13.sp,
                color = tint,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
