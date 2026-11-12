package com.example.scrollblock.android12.insta

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollblock.detector.DetectionResult
import com.example.scrollblock.detector.NodeUtils
import com.example.scrollblock.detector.PlatformDetector
import com.example.scrollblock.detector.instagram.InstagramReelsDetector
import com.example.scrollblock.preferences.BlockPreferences

/**
 * Android 12 ve Altı (Build.VERSION.SDK_INT <= 32) İçin İzole Instagram Modülü.
 *
 * - Bu sınıfta (`scrollblock/android12/insta/Android12InstagramDetector.kt`) yapılan
 *   değişiklikler yalnızca Android 12 ve altı cihazları etkiler; Android 13+ sürümlerini bozmaz.
 * - Kod tekrarını önlemek için ortak `InstagramReelsDetector` motorunu da besler.
 */
class Android12InstagramDetector(
    private val preferences: BlockPreferences,
    private val baseDetector: InstagramReelsDetector = InstagramReelsDetector(preferences)
) : PlatformDetector {

    override val platformName: String = "Instagram Reels"
    override val packageNames: Set<String> = baseDetector.packageNames

    override fun matchesPackage(packageName: String): Boolean {
        return baseDetector.matchesPackage(packageName)
    }

    override fun isEnabled(): Boolean {
        return baseDetector.isEnabled()
    }

    override fun detectShortFeed(
        rootNode: AccessibilityNodeInfo?,
        event: AccessibilityEvent?
    ): DetectionResult {
        if (rootNode == null) return DetectionResult.notDetected(platformName)

        // 1. Önce ortak güncel Instagram dedektör kurallarını çalıştır
        val sharedResult = baseDetector.detectShortFeed(rootNode, event)
        if (sharedResult.isShortFeed) {
            return sharedResult
        }

        // 2. Android 12 ve altı için ek özel/eski sürüm yedek kuralları
        if (!preferences.isInstagramEnabledSync()) {
            return DetectionResult.notDetected(platformName)
        }

        val reelsTab = NodeUtils.findBottomNavTab(rootNode, listOf("clips_tab"), listOf("reels", "klip"))
        if (NodeUtils.isNodeSelected(reelsTab)) {
            return DetectionResult.detected(
                platform = platformName,
                reason = "Android 12 Instagram Reels sekmesi -> Ana Sayfa",
                preferTabSwitch = true,
                targetTabIds = listOf("feed_tab"),
                targetHomeTabKeywords = listOf("ana sayfa", "home")
            )
        }

        return DetectionResult.notDetected(platformName)
    }
}
