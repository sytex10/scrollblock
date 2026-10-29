package com.example.scrollblock.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
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
 * Engelleme ve Spam-Korumalı Yönlendirme Servisi:
 *  1. Mola (Cooldown) aktifse servis hiçbir engelleme yapmaz, süre bitince otomatik devreye girer.
 *  2. Ana akış sekmesindeyse (Instagram Reels sekmesi / TikTok Ana Sayfa / YouTube Shorts sekmesi):
 *     -> Güvenli sekmeye geçiş yapar (Ana Sayfa / Gelen Kutusu). Uygulamadan ASLA ATMAZ.
 *  3. Anti-Spam & Takip Doğrulaması (Follow-up Verification):
 *     -> Kullanıcı art arda hızlıca (spam) sekmeye veya videoya bassa bile servis asla 8 sn
 *        uykuya geçmez; +260ms ve +580ms'de pencereyi tekrar denetleyip anında güvenli sekmeye döndürür.
 *  4. Tam ekran açılan tekil video (DM / Keşfet):
 *     -> DM tek video izni açıkken ilk video serbesttir; kaydırıldığı an GERİ ile sohbete döner.
 *     -> İzin kapalıyken açıldığı an GERİ ile sohbete döner.
 */
class ScrollBlockAccessibilityService : AccessibilityService() {

    private lateinit var preferences: BlockPreferences
    private lateinit var detectors: List<PlatformDetector>
    private val mainHandler = Handler(Looper.getMainLooper())

    private var lastTabSwitchTime: Long = 0L
    private var lastBackActionTime: Long = 0L
    private var lastGestureTapTime: Long = 0L
    private var lastEvalTime: Long = 0L

    private var lastActiveDetector: PlatformDetector? = null
    private var consecutiveTabAttempts: Int = 0

    private val followUpRunnable = Runnable {
        val detector = lastActiveDetector ?: return@Runnable
        if (!detector.isEnabled() || preferences.isBreakActiveSync()) return@Runnable
        val root = rootInActiveWindow ?: return@Runnable
        val rootPkg = root.packageName?.toString() ?: ""
        if (rootPkg.isNotEmpty() && !detector.matchesPackage(rootPkg)) return@Runnable
        evaluateAndBlock(detector, root, null)
    }

    companion object {
        private const val TAG = "ScrollBlockService"
        private const val TAB_SWITCH_COOLDOWN_MS = 200L
        private const val BACK_ACTION_COOLDOWN_MS = 550L
        private const val GESTURE_TAP_COOLDOWN_MS = 500L
        private const val EVAL_THROTTLE_MS = 80L
        private const val FOLLOW_UP_DELAY_MS = 260L

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

        // Mola / Cooldown kontrolü: Mola süresi dolana kadar koruma geçici olarak askıya alınır
        if (preferences.isBreakActiveSync()) {
            return
        }

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED
        ) return

        lastActiveDetector = detector
        val now = SystemClock.uptimeMillis()

        // Yüksek sıklıkta gelen içerik olaylarını hafifçe seyrekleştir ama son durumu kaçırmamak için takip planla
        if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            now - lastEvalTime < EVAL_THROTTLE_MS
        ) {
            scheduleFollowUp(FOLLOW_UP_DELAY_MS)
            return
        }
        lastEvalTime = now

        val rootNode = rootInActiveWindow ?: return
        evaluateAndBlock(detector, rootNode, event)
    }

    private fun evaluateAndBlock(
        detector: PlatformDetector,
        rootNode: AccessibilityNodeInfo,
        event: AccessibilityEvent?
    ) {
        try {
            val now = SystemClock.uptimeMillis()
            val result = detector.detectShortFeed(rootNode, event)

            if (!result.isShortFeed) {
                consecutiveTabAttempts = 0
                return
            }

            if (result.preferTabSwitch) {
                // Sekme değiştirme (Instagram Ana Sayfa / TikTok Gelen Kutusu)
                if (now - lastTabSwitchTime < TAB_SWITCH_COOLDOWN_MS) {
                    scheduleFollowUp(FOLLOW_UP_DELAY_MS)
                    return
                }
                consecutiveTabAttempts++
                val switched = switchToSafeTab(rootNode, result, consecutiveTabAttempts, now)
                if (switched) {
                    lastTabSwitchTime = now
                    preferences.recordBlock(detector.platformName)
                    Log.i(TAG, "TAB_SWITCH ${detector.platformName} [${result.reason}] attempt=$consecutiveTabAttempts")
                }
                // Spam tıklamalara karşı +260ms sonra pencereyi tekrar doğrula
                scheduleFollowUp(FOLLOW_UP_DELAY_MS)
            } else {
                // Tam ekran oynatıcıdan GERİ çıkma (DM Reel / YouTube Shorts)
                if (now - lastBackActionTime < BACK_ACTION_COOLDOWN_MS) {
                    scheduleFollowUp(FOLLOW_UP_DELAY_MS)
                    return
                }
                val backed = performGlobalAction(GLOBAL_ACTION_BACK)
                if (backed) {
                    lastBackActionTime = now
                    preferences.recordBlock(detector.platformName)
                    Log.i(TAG, "BACK_BLOCK ${detector.platformName} [${result.reason}]")
                }
                scheduleFollowUp(FOLLOW_UP_DELAY_MS + 150L)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating short-form feed detection", e)
        }
    }

    private fun scheduleFollowUp(delayMs: Long) {
        mainHandler.removeCallbacks(followUpRunnable)
        mainHandler.postDelayed(followUpRunnable, delayMs)
    }

    /**
     * Güvenli sekmeye geçiş.
     * Instagram ve TikTok'ta ASLA GLOBAL_ACTION_BACK çağırmaz, böylece uygulamadan atmaz.
     */
    private fun switchToSafeTab(
        root: AccessibilityNodeInfo,
        result: DetectionResult,
        attempt: Int,
        now: Long
    ): Boolean {
        val tab = NodeUtils.findBottomNavTab(root, result.targetTabIds, result.targetHomeTabKeywords)
        if (tab != null) {
            // 1. Öncelik: Erişilebilirlik ACTION_CLICK (ilk denemede veya her tek sayıda)
            if (attempt % 2 == 1 && NodeUtils.clickNode(tab)) {
                return true
            }
            // 2. Öncelik: Sekme merkezine tek dokunma (ACTION_CLICK engelleniyorsa veya spam durumunda)
            if (now - lastGestureTapTime >= GESTURE_TAP_COOLDOWN_MS) {
                NodeUtils.centerOf(tab)?.let { (x, y) ->
                    if (tapAt(x, y)) {
                        lastGestureTapTime = now
                        return true
                    }
                }
            }
            // Tekrar ACTION_CLICK dene
            if (NodeUtils.clickNode(tab)) {
                return true
            }
        }

        // Yalnızca YouTube Shorts'ta alt bar bulunamazsa GERİ tuşu kullanılır;
        // Instagram ve TikTok'ta uygulamadan atmamak için GERİ tuşu KULLANILMAZ.
        if (result.platform.contains("YouTube", ignoreCase = true) &&
            now - lastBackActionTime >= BACK_ACTION_COOLDOWN_MS
        ) {
            lastBackActionTime = now
            return performGlobalAction(GLOBAL_ACTION_BACK)
        }

        return false
    }

    private fun tapAt(x: Float, y: Float): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 45L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    override fun onInterrupt() {
        mainHandler.removeCallbacks(followUpRunnable)
        Log.w(TAG, "ScrollBlock Accessibility Service interrupted")
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(followUpRunnable)
        super.onDestroy()
        _isServiceRunning.value = false
        Log.i(TAG, "ScrollBlock Accessibility Service destroyed")
    }
}
