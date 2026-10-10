package com.example.scrollblock.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollblock.detector.DetectionResult
import com.example.scrollblock.detector.InstagramReelsDetector
import com.example.scrollblock.detector.NodeUtils
import com.example.scrollblock.detector.PlatformDetector
import com.example.scrollblock.detector.TikTokDetector
import com.example.scrollblock.detector.YouTubeShortsDetector
import com.example.scrollblock.preferences.BlockPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Engelleme stratejisi:
 *  1. Ana akış sekmesindeyse (TikTok Ana Sayfa / Instagram Reels / YouTube Shorts sekmesi)
 *     -> güvenli sekmeye geç (Gelen Kutusu / Ana Sayfa). Uygulamadan ATMAZ.
 *     a) Önce erişilebilirlik ACTION_CLICK
 *     b) Ekran değişmediyse sadece O SEKMENİN koordinatına tek bir dokunma
 *     c) Hâlâ değişmediyse son çare GERİ
 *  2. Tam ekran açılan tekil video (DM / keşfet) -> GERİ (önceki ekrana döner)
 *
 * Hayalet dokunma koruması:
 *  - Dokunma sadece alt barda bulunan ve görünen hedef sekmenin merkezine yapılır.
 *  - Aynı platformda kısa sürede 3 denemeden sonra 8 sn tüm aksiyonlar durdurulur.
 */
class ScrollBlockAccessibilityService : AccessibilityService() {

    private lateinit var preferences: BlockPreferences
    private lateinit var detectors: List<PlatformDetector>

    private var lastActionTime: Long = 0L
    private var lastEvalTime: Long = 0L

    // Ardışık deneme sayacı (tab switch başarısız olursa kademeli strateji için)
    private var attemptPlatform: String = ""
    private var attemptCount: Int = 0
    private var lastAttemptTime: Long = 0L
    private var pausedUntil: Long = 0L

    companion object {
        private const val TAG = "ScrollBlockService"
        private const val DEBOUNCE_INTERVAL_MS = 900L
        private const val EVAL_THROTTLE_MS = 250L
        private const val ATTEMPT_WINDOW_MS = 4000L
        private const val MAX_ATTEMPTS = 3
        private const val SAFETY_PAUSE_MS = 8000L

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceRunning.value = true
        preferences = BlockPreferences.getInstance(applicationContext)

        detectors = listOf(
            YouTubeShortsDetector(preferences),
            InstagramReelsDetector(preferences),
            TikTokDetector(preferences)
        )
        Log.i(TAG, "ScrollBlock Accessibility Service connected and active")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        val detector = detectors.firstOrNull { it.matchesPackage(packageName) } ?: return
        if (!detector.isEnabled()) return

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED
        ) return

        val now = SystemClock.uptimeMillis()
        if (now < pausedUntil) return
        if (now - lastActionTime < DEBOUNCE_INTERVAL_MS) return
        // Yoğun kaydırma olaylarında performans için seyrekleştir
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            now - lastEvalTime < EVAL_THROTTLE_MS
        ) return
        lastEvalTime = now

        val rootNode = rootInActiveWindow ?: return

        try {
            val result = detector.detectShortFeed(rootNode, event)
            if (!result.isShortFeed) {
                // Güvenli ekrana ulaşıldı -> sayaç sıfırlansın
                if (attemptPlatform == detector.platformName && now - lastAttemptTime > 1200L) {
                    attemptCount = 0
                }
                return
            }

            // Deneme sayacı
            if (attemptPlatform != detector.platformName || now - lastAttemptTime > ATTEMPT_WINDOW_MS) {
                attemptPlatform = detector.platformName
                attemptCount = 0
            }
            if (attemptCount >= MAX_ATTEMPTS) {
                // Bir şeyler ters gidiyor: kullanıcıyı kilitlememek için dur
                Log.w(TAG, "Safety pause on ${detector.platformName}")
                pausedUntil = now + SAFETY_PAUSE_MS
                attemptCount = 0
                return
            }
            attemptCount++
            lastAttemptTime = now
            lastActionTime = now

            val handled = if (result.preferTabSwitch) {
                switchToSafeTab(rootNode, result, attemptCount)
            } else {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }

            if (handled) {
                preferences.recordBlock(detector.platformName)
                Log.i(TAG, "BLOCKED ${detector.platformName} [${result.reason}] attempt=$attemptCount")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating short-form feed detection", e)
        }
    }

    /**
     * Güvenli sekmeye geçiş. attempt: 1 -> ACTION_CLICK, 2 -> tek dokunma, 3 -> GERİ.
     */
    private fun switchToSafeTab(
        root: AccessibilityNodeInfo,
        result: DetectionResult,
        attempt: Int
    ): Boolean {
        val tab = NodeUtils.findBottomNavTab(root, result.targetTabIds, result.targetHomeTabKeywords)

        if (tab != null && attempt == 1) {
            if (NodeUtils.clickNode(tab)) return true
            // ACTION_CLICK desteklenmiyorsa hemen dokunmaya geç
            NodeUtils.centerOf(tab)?.let { (x, y) -> if (tapAt(x, y)) return true }
        }
        if (tab != null && attempt == 2) {
            NodeUtils.centerOf(tab)?.let { (x, y) -> if (tapAt(x, y)) return true }
        }
        // Son çare
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun tapAt(x: Float, y: Float): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 40L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    override fun onInterrupt() {
        Log.w(TAG, "ScrollBlock Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false
        Log.i(TAG, "ScrollBlock Accessibility Service destroyed")
    }
}
