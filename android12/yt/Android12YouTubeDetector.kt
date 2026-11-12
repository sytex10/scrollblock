package com.example.scrollblock.android12.yt

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollblock.detector.DetectionResult
import com.example.scrollblock.detector.NodeUtils
import com.example.scrollblock.detector.PlatformDetector
import com.example.scrollblock.detector.youtube.YouTubeShortsDetector
import com.example.scrollblock.preferences.BlockPreferences

/**
 * Android 12 ve Altı (Build.VERSION.SDK_INT <= 32) İçin İzole YouTube Modülü.
 *
 * - Bu sınıfta (`scrollblock/android12/yt/Android12YouTubeDetector.kt`) yapılan
 *   değişiklikler yalnızca Android 12 ve altı cihazları etkiler; Android 13+ sürümlerini bozmaz.
 * - Kod tekrarını önlemek için ortak `YouTubeShortsDetector` motorunu da besler.
 */
class Android12YouTubeDetector(
    private val preferences: BlockPreferences,
    private val baseDetector: YouTubeShortsDetector = YouTubeShortsDetector(preferences)
) : PlatformDetector {

    override val platformName: String = "YouTube Shorts"
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

        // 1. Önce ortak güncel YouTube dedektör kurallarını çalıştır
        val sharedResult = baseDetector.detectShortFeed(rootNode, event)
        if (sharedResult.isShortFeed) {
            return sharedResult
        }

        // 2. Android 12 ve altı için ek özel/eski sürüm yedek kuralları
        if (!preferences.isYouTubeEnabledSync()) {
            return DetectionResult.notDetected(platformName)
        }

        val hasLegacyShortsView = NodeUtils.hasVisibleViewId(
            rootNode,
            "reel_recycler",
            "reel_watch_player",
            "shorts_player_view"
        )
        if (hasLegacyShortsView) {
            return DetectionResult.detected(
                platform = platformName,
                reason = "Android 12 YouTube Shorts oynatıcı -> Ana Sayfa",
                preferTabSwitch = true,
                targetTabIds = emptyList(),
                targetHomeTabKeywords = listOf("ana sayfa", "home")
            )
        }

        return DetectionResult.notDetected(platformName)
    }
}
