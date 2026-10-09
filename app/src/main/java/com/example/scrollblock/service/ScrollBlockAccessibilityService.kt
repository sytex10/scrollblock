package com.example.scrollblock.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.scrollblock.detector.InstagramReelsDetector
import com.example.scrollblock.detector.NodeUtils
import com.example.scrollblock.detector.PlatformDetector
import com.example.scrollblock.detector.TikTokDetector
import com.example.scrollblock.detector.YouTubeShortsDetector
import com.example.scrollblock.preferences.BlockPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScrollBlockAccessibilityService : AccessibilityService() {

    private lateinit var preferences: BlockPreferences
    private lateinit var detectors: List<PlatformDetector>

    // Loop prevention & debouncing
    private var lastActionTime: Long = 0L

    companion object {
        private const val TAG = "ScrollBlockService"
        // Cooldown between actions to prevent double-back app exit and recursive loops
        private const val DEBOUNCE_INTERVAL_MS = 900L

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

    /**
     * Dispatches an exact touch tap gesture on screen at (x, y).
     * Works across any view type, custom framework, or obfuscated UI.
     */
    private fun clickAtPoint(x: Float, y: Float): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        // Fast path: find if this package belongs to an active enabled detector
        val detector = detectors.firstOrNull { it.matchesPackage(packageName) } ?: return
        if (!detector.isEnabled()) return

        // Anti-loop / debounce guard: ignore events fired immediately after an action
        val now = SystemClock.uptimeMillis()
        if (now - lastActionTime < DEBOUNCE_INTERVAL_MS) {
            return
        }

        // Only inspect window state transitions and significant content changes
        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }

        val rootNode = rootInActiveWindow ?: return

        try {
            val result = detector.detectShortFeed(rootNode, event)
            if (result.isShortFeed) {
                lastActionTime = now

                var redirected = false
                // If tab switch is preferred (e.g. Reels/Shorts to Home, or TikTok to Gelen Kutusu)
                if (result.preferTabSwitch && result.targetHomeTabKeywords.isNotEmpty()) {
                    // 1. Accessibility ACTION_CLICK dene
                    val actionClicked = NodeUtils.clickNodeMatching(
                        rootNode,
                        *result.targetHomeTabKeywords.toTypedArray()
                    )

                    // 2. Ekrandaki koordinatları bulup gerçek dokunma hareketi (touch tap) yap
                    val coords = NodeUtils.getCoordinatesForNodeMatching(
                        rootNode,
                        *result.targetHomeTabKeywords.toTypedArray()
                    )

                    if (coords != null) {
                        clickAtPoint(coords.first, coords.second)
                        redirected = true
                    } else if (detector.platformName == "TikTok") {
                        // TikTok'ta Tab 4 (Gelen Kutusu) kesin olarak ekran genişliğinin %70'indedir
                        val dm = resources.displayMetrics
                        val tab4X = dm.widthPixels * 0.70f
                        val tab4Y = dm.heightPixels - (32f * dm.density)
                        clickAtPoint(tab4X, tab4Y)
                        redirected = true
                    } else if (actionClicked) {
                        redirected = true
                    }

                    if (redirected) {
                        preferences.recordBlock(detector.platformName)
                        Log.i(TAG, "Redirected to tab on ${detector.platformName} [Reason: ${result.reason}]")
                    }
                }

                // If tab redirect wasn't requested or didn't succeed, execute GLOBAL_ACTION_BACK
                if (!redirected) {
                    val success = performGlobalAction(GLOBAL_ACTION_BACK)
                    if (success) {
                        preferences.recordBlock(detector.platformName)
                        Log.i(TAG, "BLOCKED short-form feed on ${detector.platformName} [Reason: ${result.reason}]")
                    } else {
                        Log.w(TAG, "GLOBAL_ACTION_BACK failed to execute for ${detector.platformName}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating short-form feed detection", e)
        }
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
