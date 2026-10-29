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
 * ScrollBlock Erişilebilirlik Servisi:
 *  - Mola (Cooldown) aktifse koruma geçici olarak askıya alınır.
 *  - Ana akış sekmelerinde (Instagram Reels / TikTok Ana Sayfa / YouTube Shorts)
 *    yalnızca hedef sekmeye (Ana Sayfa / Gelen Kutusu) ACTION_CLICK gönderir;
 *    ekrana gereksiz fiziksel dokunma (tap) yapmaz ve kaydırmayı KESİNLİKLE bölmez.
 *  - Tam ekran açılan tekil videolarda (DM / Shorts) GERİ tuşu ile önceki ekrana döner.
 */
class ScrollBlockAccessibilityService : AccessibilityService() {

    private lateinit var preferences: BlockPreferences
    private lateinit var detectors: List<PlatformDetector>

    private var lastActionTime: Long = 0L
    private var lastEvalTime: Long = 0L

    companion object {
        private const val TAG = "ScrollBlockService"
        private const val DEBOUNCE_INTERVAL_MS = 650L
        private const val EVAL_THROTTLE_MS = 200L

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

        // Mola / Cooldown kontrolü
        if (preferences.isBreakActiveSync()) {
            return
        }

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED
        ) return

        val now = SystemClock.uptimeMillis()
        if (now - lastActionTime < DEBOUNCE_INTERVAL_MS) return

        // Yalnızca yüksek sıklıkta gelen içerik olaylarını seyrekleştir;
        // Pencere değişimi ve kaydırma olaylarını anında değerlendir.
        if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            now - lastEvalTime < EVAL_THROTTLE_MS
        ) return
        lastEvalTime = now

        val rootNode = rootInActiveWindow ?: return

        try {
            val result = detector.detectShortFeed(rootNode, event)
            if (!result.isShortFeed) {
                return
            }

            val handled = if (result.preferTabSwitch) {
                switchToSafeTab(rootNode, result)
            } else {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }

            if (handled) {
                lastActionTime = now
                preferences.recordBlock(detector.platformName)
                Log.i(TAG, "BLOCKED ${detector.platformName} [${result.reason}]")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating short-form feed detection", e)
        }
    }

    /**
     * Güvenli sekmeye geçiş:
     * Önce doğrudan erişilebilirlik ACTION_CLICK kullanır (kullanıcının kaydırma hareketini asla bölmez).
     * Yalnızca ACTION_CLICK desteklenmiyorsa sekme koordinatına tek dokunma yapar.
     */
    private fun switchToSafeTab(
        root: AccessibilityNodeInfo,
        result: DetectionResult
    ): Boolean {
        val tab = NodeUtils.findBottomNavTab(root, result.targetTabIds, result.targetHomeTabKeywords)
        if (tab != null) {
            if (NodeUtils.clickNode(tab)) return true
            NodeUtils.centerOf(tab)?.let { (x, y) -> if (tapAt(x, y)) return true }
        }

        if (result.platform.contains("YouTube", ignoreCase = true)) {
            return performGlobalAction(GLOBAL_ACTION_BACK)
        }
        return false
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
