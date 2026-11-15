package com.example.scrollblock.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.scrollblock.R
import com.example.scrollblock.preferences.BlockPreferences
import com.example.scrollblock.util.AccessibilityUtils

/**
 * Android Hızlı Ayarlar (Quick Settings) Kutucuğu — ShortBanTileService.
 *
 * - Bildirim perdesindeki Quick Settings kutucuğu ile BlockPreferences çift yönlü senkronize çalışır.
 * - Kullanıcı bildirim panelinden kutucuğa dokunarak korumayı duraklatabilir veya başlatabilir.
 * - Kapatıldığında overlay'ler anında ekrandan silinir.
 * - Uygulama içinden durum değiştiğinde `ShortBanTileService.syncTile(context)` ile kutucuk güncellenir.
 * - Tüm qsTile çağrıları null-safe olarak tasarlanmıştır.
 */
@RequiresApi(Build.VERSION_CODES.N)
class ShortBanTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        val preferences = BlockPreferences.getInstance(applicationContext)

        val isServiceRunning = AccessibilityUtils.isAccessibilityServiceEnabled(
            applicationContext
        )

        if (!isServiceRunning) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
            return
        }

        val isCurrentlyActive = preferences.isProtectionActiveSync()

        if (isCurrentlyActive) {
            // Bildirim panelinden kapatınca: Korumayı duraklat ve aktif overlay'leri anında kaldır
            preferences.setProtectionPaused(true)
            ScrollBlockAccessibilityService.clearOverlays()
            tile.state = Tile.STATE_INACTIVE
            tile.label = "ShortBan"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Durduruldu"
            }
        } else {
            // Bildirim panelinden açınca: Korumayı aktif et, mola varsa sonlandır
            preferences.setProtectionPaused(false)
            if (preferences.isBreakActiveSync()) {
                preferences.endBreak()
            }
            tile.state = Tile.STATE_ACTIVE
            tile.label = "ShortBan"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Koruma Açık"
            }
        }

        tile.updateTile()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val preferences = BlockPreferences.getInstance(applicationContext)

        val isServiceRunning = AccessibilityUtils.isAccessibilityServiceEnabled(
            applicationContext
        )

        if (!isServiceRunning) {
            tile.state = Tile.STATE_UNAVAILABLE
            tile.label = "ShortBan"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Servis Kapalı"
            }
            tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_shortban_logo)
            tile.updateTile()
            return
        }

        val isProtectionActive = preferences.isProtectionActiveSync()

        if (isProtectionActive) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "ShortBan"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Koruma Açık"
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "ShortBan"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (preferences.isBreakActiveSync()) "Mola Aktif" else "Durduruldu"
            }
        }

        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_shortban_logo)
        tile.updateTile()
    }

    companion object {
        fun syncTile(context: Context) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    requestListeningState(
                        context.applicationContext,
                        ComponentName(context.applicationContext, ShortBanTileService::class.java)
                    )
                }
            } catch (_: Exception) {
            }
        }
    }
}
