package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.service.FloatingAssistantService
import com.example.ui.theme.AiBubbleGradientEnd
import com.example.ui.theme.AiBubbleGradientStart
import com.example.ui.viewmodel.ChatViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScreenAssistantHubScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val isServiceRunning by FloatingAssistantService.isRunningFlow.collectAsState()

    val adminSettings by viewModel.adminSettings.collectAsState()

    var customTextDraft by remember(adminSettings.bubbleText) {
        mutableStateOf(adminSettings.bubbleText)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setCustomBubbleImage(uri, context)
        }
    }



    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        startFloatingService(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Floating Assistant & Screen AI",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master Switch Banner Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isServiceRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isServiceRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("floating_service_master_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        brush = Brush.linearGradient(
                                            listOf(AiBubbleGradientStart, AiBubbleGradientEnd)
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPictureAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Floating AI Bubble",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (isServiceRunning) "Active over other apps" else "Disabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isServiceRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isServiceRunning,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    if (!Settings.canDrawOverlays(context)) {
                                        Toast.makeText(context, "Please grant 'Display over other apps' permission first", Toast.LENGTH_LONG).show()
                                        requestOverlayPermission(context)
                                    } else {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                        ) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            startFloatingService(context)
                                        }
                                    }
                                } else {
                                    try {
                                        val intent = Intent(context, FloatingAssistantService::class.java).apply {
                                            action = FloatingAssistantService.ACTION_STOP_SERVICE
                                        }
                                        context.startService(intent)
                                        context.stopService(intent)
                                    } catch (e: Exception) {}
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("floating_service_switch")
                        )
                    }

                    if (!hasOverlayPermission) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "⚠️ Permission Required",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "To show the floating bubble and Samsung Pop-up window over YouTube, WhatsApp, or games, please grant 'Display over other apps' permission.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { requestOverlayPermission(context) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.testTag("grant_overlay_permission_button")
                                ) {
                                    Text("Grant Overlay Permission")
                                }
                            }
                        }
                    }


                }
            }

            // ==========================================
            // FLOATING ICON & AVATAR CUSTOMIZATION (USER REQUEST)
            // ==========================================
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bubble_customization_section")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Floating Icon & Avatar Customization",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Change photo, avatar, emoji, text & shape",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Live Interactive Bubble Preview
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF1E1E2C),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val previewAlpha = adminSettings.bubbleAlpha.coerceIn(0.15f, 1.0f)
                                Text(
                                    text = "📱 LIVE PREVIEW",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.85f),
                                    letterSpacing = 1.sp
                                )

                                // Render Preview Widget
                                val hasCustomPhoto = adminSettings.bubbleCustomImagePath.isNotBlank() && File(adminSettings.bubbleCustomImagePath).exists()
                                val gradientColors = when (adminSettings.bubbleGradient.lowercase()) {
                                    "cyan" -> listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
                                    "sunset" -> listOf(Color(0xFFFF512F), Color(0xFFDD2476))
                                    "emerald" -> listOf(Color(0xFF11998E), Color(0xFF38EF7D))
                                    "dark" -> listOf(Color(0xFF232526), Color(0xFF414345))
                                    "gold" -> listOf(Color(0xFFF7971E), Color(0xFFFFD200))
                                    else -> listOf(AiBubbleGradientStart, AiBubbleGradientEnd)
                                }

                                val iconVec = when (adminSettings.bubblePresetIcon.lowercase()) {
                                    "robot" -> Icons.Default.SmartToy
                                    "brain" -> Icons.Default.Psychology
                                    "flash" -> Icons.Default.Bolt
                                    "target" -> Icons.Default.RadioButtonChecked
                                    "flame" -> Icons.Default.Whatshot
                                    "diamond" -> Icons.Default.Diamond
                                    "eye" -> Icons.Default.RemoveRedEye
                                    else -> Icons.Default.AutoAwesome
                                }

                                Box(
                                    modifier = Modifier.alpha(previewAlpha)
                                ) {
                                    if (adminSettings.bubbleStyle == "circle") {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .background(Brush.linearGradient(gradientColors), CircleShape)
                                                .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (hasCustomPhoto) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(File(adminSettings.bubbleCustomImagePath))
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = "Preview",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape)
                                                )
                                            } else {
                                                Icon(imageVector = iconVec, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                            }
                                        }
                                    } else if (adminSettings.bubbleStyle == "icon_only" || adminSettings.bubbleStyle == "square") {
                                        val sqShape = RoundedCornerShape(14.dp)
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .background(Brush.linearGradient(gradientColors), sqShape)
                                                .border(1.5.dp, Color.White.copy(alpha = 0.7f), sqShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (hasCustomPhoto) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(File(adminSettings.bubbleCustomImagePath))
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = "Preview",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(sqShape)
                                                )
                                            } else {
                                                Icon(imageVector = iconVec, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                            }
                                        }
                                    } else {
                                        val pillShape = RoundedCornerShape(26.dp)
                                        Box(
                                            modifier = Modifier
                                                .background(Brush.linearGradient(gradientColors), pillShape)
                                                .border(1.5.dp, Color.White.copy(alpha = 0.6f), pillShape)
                                                .padding(horizontal = 16.dp, vertical = 9.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (hasCustomPhoto) {
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(context)
                                                            .data(File(adminSettings.bubbleCustomImagePath))
                                                            .crossfade(true)
                                                            .build(),
                                                        contentDescription = "Preview",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(22.dp)
                                                            .clip(CircleShape)
                                                            .border(1.dp, Color.White, CircleShape)
                                                    )
                                                } else {
                                                    Icon(imageVector = iconVec, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                                }

                                                Text(
                                                    text = if (adminSettings.bubbleText.isNotBlank()) adminSettings.bubbleText else "AI ✨",
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Your Custom Photo / Avatar Image
                    Text(
                        text = "Your Custom Photo / Avatar Image",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("pick_custom_bubble_image_button")
                        ) {
                            Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Choose Photo")
                        }

                        if (adminSettings.bubbleCustomImagePath.isNotBlank()) {
                            FilledTonalButton(
                                onClick = { viewModel.removeCustomBubbleImage(context) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(46.dp)
                                    .testTag("remove_custom_bubble_image_button")
                            ) {
                                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset Photo")
                            }
                        }
                    }

                    // Preset AI Icons
                    Text(
                        text = "Preset AI Icon / Mascot",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val presets = listOf(
                            Pair("sparkle", "✨ Sparkle"),
                            Pair("robot", "🤖 Robot"),
                            Pair("brain", "🧠 Brain"),
                            Pair("flash", "⚡ Flash"),
                            Pair("target", "🎯 Focus"),
                            Pair("flame", "🔥 Fire"),
                            Pair("diamond", "💎 Gem"),
                            Pair("eye", "👁️ Vision")
                        )

                        presets.forEach { (key, label) ->
                            FilterChip(
                                selected = adminSettings.bubblePresetIcon == key && adminSettings.bubbleCustomImagePath.isBlank(),
                                onClick = {
                                    if (adminSettings.bubbleCustomImagePath.isNotBlank()) {
                                        viewModel.removeCustomBubbleImage(context)
                                    }
                                    viewModel.updateAdminSettings(bubblePresetIcon = key)
                                },
                                label = { Text(label, fontSize = 12.5.sp) }
                            )
                        }
                    }

                    // Shape & Style
                    Text(
                        text = "Bubble Shape",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = adminSettings.bubbleStyle == "pill",
                            onClick = { viewModel.updateAdminSettings(bubbleStyle = "pill") },
                            label = { Text("Pill (Icon + Text)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = adminSettings.bubbleStyle == "circle",
                            onClick = { viewModel.updateAdminSettings(bubbleStyle = "circle") },
                            label = { Text("Circle Avatar") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = adminSettings.bubbleStyle == "square",
                            onClick = { viewModel.updateAdminSettings(bubbleStyle = "square") },
                            label = { Text("Squircle Badge") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Custom Text (if Pill style)
                    if (adminSettings.bubbleStyle == "pill") {
                        Text(
                            text = "Custom Label Text",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customTextDraft,
                                onValueChange = { customTextDraft = it },
                                placeholder = { Text("e.g. AI ✨, Jarvis, Scan") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("bubble_custom_text_input")
                            )

                            Button(
                                onClick = {
                                    viewModel.updateAdminSettings(bubbleText = customTextDraft.ifBlank { "AI ✨" })
                                    Toast.makeText(context, "Text updated!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(54.dp)
                            ) {
                                Text("Save")
                            }
                        }
                    }

                    // Color Themes
                    Text(
                        text = "Gradient Color Theme",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val themes = listOf(
                            Pair("purple", "💜 Neon Purple"),
                            Pair("cyan", "🩵 Cyber Cyan"),
                            Pair("sunset", "🌅 Sunset Flame"),
                            Pair("emerald", "💚 Emerald Matrix"),
                            Pair("gold", "👑 Royal Gold"),
                            Pair("dark", "🖤 AMOLED Dark")
                        )

                        themes.forEach { (key, label) ->
                            FilterChip(
                                selected = adminSettings.bubbleGradient == key,
                                onClick = { viewModel.updateAdminSettings(bubbleGradient = key) },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }

                    // Size Selection
                    Text(
                        text = "Size on Screen",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = adminSettings.bubbleSize == "small",
                            onClick = { viewModel.updateAdminSettings(bubbleSize = "small") },
                            label = { Text("Small") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = adminSettings.bubbleSize == "medium",
                            onClick = { viewModel.updateAdminSettings(bubbleSize = "medium") },
                            label = { Text("Medium") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = adminSettings.bubbleSize == "large",
                            onClick = { viewModel.updateAdminSettings(bubbleSize = "large") },
                            label = { Text("Large") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 7. Bubble Transparency / Opacity (पारदर्शिता)
                    Text(
                        text = "Bubble Transparency",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Slider(
                        value = adminSettings.bubbleAlpha.coerceIn(0.15f, 1.0f),
                        onValueChange = { newAlpha ->
                            viewModel.updateAdminSettings(bubbleAlpha = newAlpha)
                        },
                        valueRange = 0.15f..1.0f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Interactive Feature Highlights
            Text(
                text = "Key Floating Capabilities",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            FeatureCard(
                icon = Icons.Default.TextFields,
                title = "Live OCR Screen Text Grabber (Feature 1)",
                description = "Extract all readable text, questions, or articles directly from your screen into interactive blocks with 1-tap Copy All, Copy Line, and Translate.",
                accentColor = MaterialTheme.colorScheme.tertiary
            )

            FeatureCard(
                icon = Icons.Default.AutoAwesome,
                title = "Auto-Floating Quick Solution HUD (Feature 2)",
                description = "Get instant concise answers, formulas, or summaries floating cleanly in a compact card over games, PDFs, and videos without covering your screen.",
                accentColor = MaterialTheme.colorScheme.primary
            )

            FeatureCard(
                icon = Icons.Default.Layers,
                title = "Pop-up Window (Lag-Free & Resizable)",
                description = "Open a smooth, draggable floating window. Move it around or resize using the corner handle without any lag or freezing.",
                accentColor = MaterialTheme.colorScheme.secondary
            )

            FeatureCard(
                icon = Icons.Default.Screenshot,
                title = "Full & Area Crop Screen Scan",
                description = "Instant full display scan or finger-drag cropped area scan with optimized background threads.",
                accentColor = MaterialTheme.colorScheme.primary
            )

            // Direct Test Actions
            Text(
                text = "Test & Action Controls",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Button(
                onClick = {
                    if (!Settings.canDrawOverlays(context)) {
                        Toast.makeText(context, "Please grant 'Display over other apps' permission first", Toast.LENGTH_LONG).show()
                        requestOverlayPermission(context)
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            startFloatingService(context)
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("launch_bubble_button")
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Launch Bubble")
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun startFloatingService(context: Context) {
    try {
        val intent = Intent(context, FloatingAssistantService::class.java).apply {
            action = FloatingAssistantService.ACTION_SHOW_BUBBLE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        Toast.makeText(context, "Floating Assistant Bubble started!", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to start service: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

private fun requestOverlayPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
