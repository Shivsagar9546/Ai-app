package com.example.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.R

@RequiresApi(Build.VERSION_CODES.N)
class OmniAiQuickSettingsTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        // 1. Overlay permission check
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(
                this,
                "Please enable 'Display over other apps' to open OmniAI Floating Chat",
                Toast.LENGTH_LONG
            ).show()

            val overlayIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            if (Build.VERSION.SDK_INT >= 34) {
                // Android 14+ requires PendingIntent or safe startActivityAndCollapse
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    101,
                    overlayIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(overlayIntent)
            }
            return
        }

        // 2. Collapse notification shade and launch or toggle floating chat
        unlockAndRun {
            val serviceIntent = Intent(this, FloatingAssistantService::class.java).apply {
                action = FloatingAssistantService.ACTION_CHAT
            }

            val activeInstance = FloatingAssistantService.activeServiceInstance
            if (activeInstance != null) {
                if (activeInstance.isPopupShowing) {
                    activeInstance.minimizePopup()
                } else {
                    activeInstance.showPopup()
                }
            } else {
                try {
                    startService(serviceIntent)
                } catch (_: Exception) {}
            }

            updateTileState()
        }
    }

    private fun updateTileState() {
        val currentTile = qsTile ?: return
        val activeInstance = FloatingAssistantService.activeServiceInstance
        val isServiceRunning = activeInstance != null
        val isPopupVisible = activeInstance?.isPopupShowing == true

        currentTile.state = if (isPopupVisible || isServiceRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        currentTile.label = getString(R.string.qs_tile_omni_ai)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            currentTile.subtitle = when {
                isPopupVisible -> "Chat open"
                isServiceRunning -> "Floating active"
                else -> "Tap to open chat"
            }
        }
        currentTile.icon = Icon.createWithResource(this, R.drawable.ic_qs_omni_ai)
        currentTile.updateTile()
    }
}
