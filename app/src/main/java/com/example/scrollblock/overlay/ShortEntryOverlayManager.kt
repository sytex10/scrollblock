package com.example.scrollblock.overlay

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Kısa Video Girişlerini Gizle (Arayüz Koruma Sistemi) — Accessibility Overlay Yöneticisi.
 *
 * - `WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY` kullanır; bu sayede ekstra
 *   `SYSTEM_ALERT_WINDOW` izni istemez ve Google Play politikalarıyla tam uyumludur.
 * - Sadece tespit edilen kısa video sekme ikonunun (`Rect`) üzerini hedef uygulamanın
 *   o anki Karanlık/Aydınlık mod zemin rengiyle örter ve o noktadaki parmak dokunuşlarını
 *   etkisizleştirir (`OnTouchListener { _, _ -> true }`).
 * - Hedef uygulamadan çıkıldığında, klavye açıldığında veya öğe görünmez olduğunda
 *   milisaniyeler içinde kendini gizler (`clearAllOverlays`).
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

    private data class OverlayEntry(
        val view: View,
        val params: WindowManager.LayoutParams,
        var lastBounds: Rect,
        var lastColor: Int
    )

    private val activeOverlays = mutableListOf<OverlayEntry>()

    fun updateForWindow(
        rootNode: AccessibilityNodeInfo?,
        packageName: String,
        isEnabled: Boolean,
        blockInstagram: Boolean,
        blockYoutube: Boolean,
        blockTiktok: Boolean
    ) {
        if (!isEnabled || rootNode == null) {
            clearAllOverlays()
            return
        }

        val (screenW, screenH) = getScreenDimensions()
        if (screenW <= 0 || screenH <= 0) {
            clearAllOverlays()
            return
        }

        val targets = ShortEntryLocator.findTargetsToMask(
            context = service,
            rootNode = rootNode,
            packageName = packageName,
            screenWidth = screenW,
            screenHeight = screenH,
            blockInstagram = blockInstagram,
            blockYoutube = blockYoutube,
            blockTiktok = blockTiktok
        )

        if (targets.isEmpty()) {
            clearAllOverlays()
            return
        }

        mainHandler.post {
            applyTargetsOnUiThread(targets)
        }
    }

    fun clearAllOverlays() {
        mainHandler.post {
            removeAllOnUiThread()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun applyTargetsOnUiThread(targets: List<ShortEntryLocator.MaskedTarget>) {
        val wm = windowManager ?: return

        try {
            // Fazla olan overlay görünümlerini kaldır
            while (activeOverlays.size > targets.size) {
                val removed = activeOverlays.removeAt(activeOverlays.lastIndex)
                try {
                    wm.removeViewImmediate(removed.view)
                } catch (_: Exception) {
                }
            }

            // Mevcutları güncelle veya yeni ekle
            targets.forEachIndexed { index, target ->
                val rect = target.bounds
                if (rect.width() <= 0 || rect.height() <= 0) return@forEachIndexed

                if (index < activeOverlays.size) {
                    val existing = activeOverlays[index]
                    val boundsChanged = existing.lastBounds != rect
                    val colorChanged = existing.lastColor != target.backgroundColor

                    if (colorChanged) {
                        existing.view.setBackgroundColor(target.backgroundColor)
                        existing.lastColor = target.backgroundColor
                    }

                    if (boundsChanged) {
                        existing.params.x = rect.left
                        existing.params.y = rect.top
                        existing.params.width = rect.width()
                        existing.params.height = rect.height()
                        existing.lastBounds = Rect(rect)
                        try {
                            wm.updateViewLayout(existing.view, existing.params)
                        } catch (_: Exception) {
                        }
                    }
                } else {
                    val maskView = View(service).apply {
                        setBackgroundColor(target.backgroundColor)
                        // Parmak dokunuşlarını yutarak kısa video sekmesine girişi engelle
                        setOnTouchListener { _, _ -> true }
                        isClickable = true
                        isFocusable = false
                    }

                    val params = createOverlayLayoutParams(rect)
                    try {
                        wm.addView(maskView, params)
                        activeOverlays.add(
                            OverlayEntry(
                                view = maskView,
                                params = params,
                                lastBounds = Rect(rect),
                                lastColor = target.backgroundColor
                            )
                        )
                    } catch (_: Exception) {
                        // Zarif Bozulma: Pencere eklenemezse sessizce geç
                    }
                }
            }
        } catch (_: Exception) {
            removeAllOnUiThread()
        }
    }

    private fun createOverlayLayoutParams(rect: Rect): WindowManager.LayoutParams {
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        return WindowManager.LayoutParams(
            rect.width(),
            rect.height(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = rect.left
            y = rect.top
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun removeAllOnUiThread() {
        val wm = windowManager
        val iterator = activeOverlays.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            try {
                wm?.removeViewImmediate(item.view)
            } catch (_: Exception) {
            }
            iterator.remove()
        }
    }

    @Suppress("DEPRECATION")
    private fun getScreenDimensions(): Pair<Int, Int> {
        val wm = windowManager ?: return 0 to 0
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = wm.currentWindowMetrics.bounds
                bounds.width() to bounds.height()
            } else {
                val dm = DisplayMetrics()
                wm.defaultDisplay?.getRealMetrics(dm)
                dm.widthPixels to dm.heightPixels
            }
        } catch (_: Exception) {
            val dm = service.resources.displayMetrics
            dm.widthPixels to dm.heightPixels
        }
    }
}

