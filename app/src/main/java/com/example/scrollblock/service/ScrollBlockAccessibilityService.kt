package com.example.scrollblock.service

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollblock.android12.core.Android12ServiceRouter
import com.example.scrollblock.detector.DetectionResult
import com.example.scrollblock.detector.NodeUtils
import com.example.scrollblock.detector.PlatformDetector
import com.example.scrollblock.detector.instagram.InstagramReelsDetector
import com.example.scrollblock.detector.tiktok.TikTokDetector
import com.example.scrollblock.detector.youtube.YouTubeShortsDetector
import com.example.scrollblock.overlay.ShortEntryOverlayManager
import com.example.scrollblock.preferences.BlockPreferences
import com.example.scrollblock.updater.UpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ScrollBlock Erişilebilirlik Servisi:
 *  - Mola (Cooldown) aktifse koruma geçici olarak askıya alınır.
 *  - Ana akış sekmelerinde (Instagram Reels / TikTok Ana Sayfa / YouTube Shorts)
 *    yalnızca hedef sekmeye (Ana Sayfa / Gelen Kutusu) ACTION_CLICK gönderir;
 *    ekrana fiziksel dokunma (gesture) yapmaz ve kaydırmayı KESİNLİKLE bölmez.
 *  - Tam ekran açılan tekil videolarda (DM / Shorts) GERİ tuşu ile önceki ekrana döner.
 */
class ScrollBlockAccessibilityService : AccessibilityService() {

    private lateinit var preferences: BlockPreferences
    private lateinit var detectors: List<PlatformDetector>
    private var overlayManager: ShortEntryOverlayManager? = null

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
        // Güncelleme kurulduğunda servis yeniden bağlanır; kurulum tamamlandıysa indirilen geçici APK'yı otomatik temizle
        UpdateManager.cleanupInstalledApks(applicationContext, forceAll = false)
        preferences = BlockPreferences.getInstance(applicationContext)
        overlayManager = ShortEntryOverlayManager(this)

        detectors = if (Android12ServiceRouter.isAndroid12OrBelow()) {
            Log.i(TAG, "Initializing isolated Android 12 and below (SDK <= 32) detector modules")
            Android12ServiceRouter.createAndroid12Detectors(preferences)
        } else {
            listOf(
                YouTubeShortsDetector(preferences),
                InstagramReelsDetector(preferences),
                TikTokDetector(preferences)
            )
        }
        Log.i(TAG, "ScrollBlock Accessibility Service connected and active")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        // Kendi overlay katmanımızdan gelen olayları yoksay
        if (packageName == applicationContext.packageName) return

        val eventType = event.eventType
        val detector = detectors.firstOrNull { it.matchesPackage(packageName) }

        if (detector == null) {
            return
        }

        // Mola / Cooldown kontrolü
        if (preferences.isBreakActiveSync()) {
            return
        }

        if (!detector.isEnabled()) {
            return
        }

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
            // Akış ve Yorum Engelleme Motoru (Tıklanınca Anında Engelleme)
            if (!detector.isEnabled()) return

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
     * Doğrudan erişilebilirlik ACTION_CLICK kullanır (ekrana fiziksel dokunma göndermez).
     */
    private fun switchToSafeTab(
        root: AccessibilityNodeInfo,
        result: DetectionResult
    ): Boolean {
        val tab = NodeUtils.findBottomNavTab(root, result.targetTabIds, result.targetHomeTabKeywords)
        if (tab != null && NodeUtils.clickNode(tab)) {
            return true
        }

        if (result.platform.contains("YouTube", ignoreCase = true)) {
            return performGlobalAction(GLOBAL_ACTION_BACK)
        }
        return false
    }

    override fun onInterrupt() {
        overlayManager?.clearAllOverlays()
        Log.w(TAG, "ScrollBlock Accessibility Service interrupted")
    }

    override fun onDestroy() {
        overlayManager?.clearAllOverlays()
        super.onDestroy()
        _isServiceRunning.value = false
        Log.i(TAG, "ScrollBlock Accessibility Service destroyed")
    }
}
