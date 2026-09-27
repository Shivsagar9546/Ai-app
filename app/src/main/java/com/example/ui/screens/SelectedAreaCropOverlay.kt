package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

@Composable
fun SelectedAreaCropOverlay(
    backgroundImage: Bitmap? = null,
    onAreaSelected: (Rect?) -> Unit,
    onCancel: () -> Unit
) {
    var startOffset by remember { mutableStateOf<Offset?>(null) }
    var currentOffset by remember { mutableStateOf<Offset?>(null) }
    var overlayWindowOffset by remember { mutableStateOf(Offset.Zero) }
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                overlayWindowOffset = coordinates.positionInWindow()
                overlaySize = coordinates.size
            }
            .testTag("area_crop_overlay")
    ) {
        // Draw standard user-supplied background screenshot cleanly and safely
        backgroundImage?.takeIf { !it.isRecycled }?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Background Screenshot",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }

        // 1. Transparent Gesture Detection & High-Performance Drawing Canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            startOffset = offset
                            currentOffset = offset
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentOffset = change.position
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val start = startOffset
                val end = currentOffset
                val scrimColor = Color(0x99000000)

                if (start != null && end != null) {
                    val left = min(start.x, end.x).coerceAtLeast(0f)
                    val top = min(start.y, end.y).coerceAtLeast(0f)
                    val right = max(start.x, end.x).coerceIn(left, size.width)
                    val bottom = max(start.y, end.y).coerceIn(top, size.height)
                    val rectW = right - left
                    val rectH = bottom - top

                    if (rectW > 15f && rectH > 15f) {
                        // Four outer scrim rectangles to cleanly frame transparent crop area without BlendMode.Clear crashes
                        if (top > 0f) {
                            drawRect(color = scrimColor, topLeft = Offset(0f, 0f), size = Size(size.width, top))
                        }
                        if (size.height > bottom) {
                            drawRect(color = scrimColor, topLeft = Offset(0f, bottom), size = Size(size.width, size.height - bottom))
                        }
                        if (left > 0f) {
                            drawRect(color = scrimColor, topLeft = Offset(0f, top), size = Size(left, rectH))
                        }
                        if (size.width > right) {
                            drawRect(color = scrimColor, topLeft = Offset(right, top), size = Size(size.width - right, rectH))
                        }

                        // Vibrant Bounding Box
                        drawRect(
                            color = Color(0xFF6366F1),
                            topLeft = Offset(left, top),
                            size = Size(rectW, rectH),
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // 4 High-visibility Corner Brackets
                        val cornerLen = 22.dp.toPx()
                        val strokeW = 5.dp.toPx()
                        val cyan = Color(0xFF38BDF8)
                        // Top-Left
                        drawLine(cyan, Offset(left, top), Offset(left + cornerLen, top), strokeW)
                        drawLine(cyan, Offset(left, top), Offset(left, top + cornerLen), strokeW)
                        // Top-Right
                        drawLine(cyan, Offset(right, top), Offset(right - cornerLen, top), strokeW)
                        drawLine(cyan, Offset(right, top), Offset(right, top + cornerLen), strokeW)
                        // Bottom-Left
                        drawLine(cyan, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeW)
                        drawLine(cyan, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeW)
                        // Bottom-Right
                        drawLine(cyan, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeW)
                        drawLine(cyan, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeW)
                    } else {
                        drawRect(color = scrimColor, size = size)
                    }
                } else {
                    drawRect(color = scrimColor, size = size)
                }
            }
        }

        // 2. Top instruction pill
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CropFree,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (startOffset != null && currentOffset != null) "Area selected! Tap Scan below" else "Drag finger over question or area",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (startOffset != null) {
                    IconButton(
                        onClick = {
                            startOffset = null
                            currentOffset = null
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Selection",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 3. Bottom action buttons (placed on top layer so they are 100% clickable)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp, start = 20.dp, end = 20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("crop_cancel_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val start = startOffset
                        val end = currentOffset
                        if (start != null && end != null) {
                            val selLeft = min(start.x, end.x) + overlayWindowOffset.x
                            val selTop = min(start.y, end.y) + overlayWindowOffset.y
                            val selRight = max(start.x, end.x) + overlayWindowOffset.x
                            val selBottom = max(start.y, end.y) + overlayWindowOffset.y

                            val selWidth = selRight - selLeft
                            val selHeight = selBottom - selTop

                            if (selWidth > 20f && selHeight > 20f) {
                                // Obtain actual display pixel metrics for accurate scaling
                                val wm = context.getSystemService(android.content.Context.WINDOW_SERVICE) as? android.view.WindowManager
                                val displayMetrics = android.util.DisplayMetrics()
                                @Suppress("DEPRECATION")
                                wm?.defaultDisplay?.getRealMetrics(displayMetrics)
                                val realDisplayW = displayMetrics.widthPixels
                                val realDisplayH = displayMetrics.heightPixels

                                val finalRect = if (overlaySize.width > 0 && overlaySize.height > 0 && realDisplayW > 0 && realDisplayH > 0) {
                                    val scaleX = realDisplayW.toFloat() / overlaySize.width.toFloat()
                                    val scaleY = realDisplayH.toFloat() / overlaySize.height.toFloat()

                                    val pxLeft = (selLeft * scaleX).toInt().coerceIn(0, realDisplayW - 1)
                                    val pxTop = (selTop * scaleY).toInt().coerceIn(0, realDisplayH - 1)
                                    val pxRight = (selRight * scaleX).toInt().coerceIn(pxLeft + 1, realDisplayW)
                                    val pxBottom = (selBottom * scaleY).toInt().coerceIn(pxTop + 1, realDisplayH)
                                    Rect(pxLeft, pxTop, pxRight, pxBottom)
                                } else {
                                    Rect(selLeft.toInt(), selTop.toInt(), selRight.toInt(), selBottom.toInt())
                                }

                                onAreaSelected(finalRect)
                                return@Button
                            }
                        }
                        // Fallback to full screen if minimal area selected
                        onAreaSelected(null)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("crop_confirm_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirm",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan Area", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
