package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MainChatScreen
import com.example.ui.screens.ScreenAssistantHubScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.OmniAITheme
import com.example.ui.viewmodel.ChatViewModel

object AppRoutes {
    const val CHAT = "chat"
    const val FLOATING_HUB = "floating_hub"
    const val HISTORY = "history"
    const val ADMIN = "admin"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels()
    private val navTargetState = androidx.compose.runtime.mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialConvId = intent?.getStringExtra("CONVERSATION_ID")
        val initialNavTarget = intent?.getStringExtra("NAV_TARGET")

        if (!initialConvId.isNullOrBlank()) {
            viewModel.selectConversation(initialConvId)
        }
        navTargetState.value = initialNavTarget

        handleIncomingIntent(intent)

        setContent {
            val adminSettings by viewModel.adminSettings.collectAsState()
            val isDark = when (adminSettings.appTheme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            OmniAITheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OmniAIAppNavigation(
                        viewModel = viewModel,
                        navTarget = navTargetState.value,
                        onNavTargetConsumed = { navTargetState.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val convId = intent.getStringExtra("CONVERSATION_ID")
        if (!convId.isNullOrBlank()) {
            viewModel.selectConversation(convId)
        }
        val target = intent.getStringExtra("NAV_TARGET")
        if (!target.isNullOrBlank()) {
            navTargetState.value = target
        }
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type

        when (action) {
            Intent.ACTION_SEND -> {
                if (type?.startsWith("image/") == true) {
                    val streamUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    }
                    val clipUri = intent.clipData?.let { cd ->
                        if (cd.itemCount > 0) cd.getItemAt(0).uri else null
                    }
                    val targetUri = streamUri ?: clipUri
                    if (targetUri != null) {
                        viewModel.attachMultipleImageUris(listOf(targetUri))
                    }
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrBlank()) {
                        viewModel.setPendingSharedText(sharedText)
                    }
                } else if (type?.startsWith("text/") == true || type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrBlank()) {
                        viewModel.setPendingSharedText(sharedText)
                    }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                if (type?.startsWith("image/") == true) {
                    val uris = mutableListOf<Uri>()
                    val streamUris: ArrayList<Uri>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                    }
                    if (streamUris != null) {
                        uris.addAll(streamUris)
                    }
                    intent.clipData?.let { clipData ->
                        for (i in 0 until clipData.itemCount) {
                            clipData.getItemAt(i).uri?.let { u ->
                                if (!uris.contains(u)) uris.add(u)
                            }
                        }
                    }
                    if (uris.isNotEmpty()) {
                        viewModel.attachMultipleImageUris(uris)
                    }
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrBlank()) {
                        viewModel.setPendingSharedText(sharedText)
                    }
                }
            }
        }
    }
}

@Composable
fun OmniAIAppNavigation(
    viewModel: ChatViewModel,
    navTarget: String?,
    onNavTargetConsumed: () -> Unit
) {
    val navController = rememberNavController()

    LaunchedEffect(navTarget) {
        if (!navTarget.isNullOrBlank()) {
            when (navTarget) {
                "settings" -> navController.navigate(AppRoutes.SETTINGS)
                "floating_hub", "assistant_hub" -> navController.navigate(AppRoutes.FLOATING_HUB)
            }
            onNavTargetConsumed()
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoutes.CHAT
    ) {
        composable(AppRoutes.CHAT) {
            MainChatScreen(
                viewModel = viewModel,
                onNavigateToHistory = { navController.navigate(AppRoutes.HISTORY) },
                onNavigateToFloatingHub = { navController.navigate(AppRoutes.FLOATING_HUB) },
                onNavigateToAdmin = { navController.navigate(AppRoutes.ADMIN) },
                onNavigateToSettings = { navController.navigate(AppRoutes.SETTINGS) }
            )
        }

        composable(AppRoutes.FLOATING_HUB) {
            ScreenAssistantHubScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.HISTORY) {
            HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onSelectConversation = {
                    navController.popBackStack(AppRoutes.CHAT, inclusive = false)
                }
            )
        }

        composable(AppRoutes.ADMIN) {
            AdminPanelScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAdmin = { navController.navigate(AppRoutes.ADMIN) }
            )
        }
    }
}
