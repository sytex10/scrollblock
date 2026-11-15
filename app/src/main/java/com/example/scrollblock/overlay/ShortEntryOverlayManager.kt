package com.example.scrollblock.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Kısa Video Girişlerini Gizle — Accessibility Overlay Yöneticisi.
 *
 * Kullanıcının ekran dokunmatiğinin (özellikle alt bar, klavye, geri ve ev butonları)
 * engellenmesini veya kilitlenmesini %100 önlemek için ekran üzerine dokunma engelleyici
 * katman ekleme işlemi tamamen devre dışı bırakılmıştır.
 */
class ShortEntryOverlayManager(private val service: AccessibilityService) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val windowManager: WindowManager? by lazy {
        try {
            service.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        } catch (_: Exception) {
            null
        }
    }

    private val activeOverlays = mutableListOf<View>()

    fun updateForWindow(
        rootNode: AccessibilityNodeInfo?,
        packageName: String,
        isEnabled: Boolean,
        blockInstagram: Boolean,
        blockYoutube: Boolean,
        blockTiktok: Boolean
    ) {
        clearAllOverlays()
    }

    fun clearAllOverlays() {
        mainHandler.post {
            removeAllOnUiThread()
        }
    }

    private fun removeAllOnUiThread() {
        val wm = windowManager ?: return
        val iterator = activeOverlays.iterator()
        while (iterator.hasNext()) {
            val view = iterator.next()
            try {
                wm.removeViewImmediate(view)
            } catch (_: Exception) {
            }
            iterator.remove()
        }
    }
}
