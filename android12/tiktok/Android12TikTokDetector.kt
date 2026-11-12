package com.example.scrollblock.android12.tiktok

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollblock.detector.DetectionResult
import com.example.scrollblock.detector.NodeUtils
import com.example.scrollblock.detector.PlatformDetector
import com.example.scrollblock.detector.tiktok.TikTokDetector
import com.example.scrollblock.preferences.BlockPreferences

/**
 * Android 12 ve Altı (Build.VERSION.SDK_INT <= 32) İçin İzole TikTok Modülü.
 *
 * - Bu sınıfta (`scrollblock/android12/tiktok/Android12TikTokDetector.kt`) yapılan
 *   değişiklikler yalnızca Android 12 ve altı cihazları etkiler; Android 13+ sürümlerini bozmaz.
 * - Kod tekrarını önlemek için ortak `TikTokDetector` motorunu da besler.
 */
class Android12TikTokDetector(
    private val preferences: BlockPreferences,
    private val baseDetector: TikTokDetector = TikTokDetector(preferences)
) : PlatformDetector {

    override val platformName: String = "TikTok"
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

        // 1. Önce ortak güncel TikTok dedektör kurallarını çalıştır
        val sharedResult = baseDetector.detectShortFeed(rootNode, event)
        if (sharedResult.isShortFeed) {
            return sharedResult
        }

        // 2. Android 12 ve altı için ek özel/eski sürüm yedek kuralları
        if (!preferences.isTikTokEnabledSync()) {
            return DetectionResult.notDetected(platformName)
        }

        val homeTab = NodeUtils.findBottomNavTab(rootNode, emptyList(), listOf("ana sayfa", "home"))
        val friendsTab = NodeUtils.findBottomNavTab(rootNode, emptyList(), listOf("arkadaşlar", "friends"))
        if (NodeUtils.isNodeSelected(homeTab) || NodeUtils.isNodeSelected(friendsTab)) {
            return DetectionResult.detected(
                platform = platformName,
                reason = "Android 12 TikTok Video Akışı (Ana Sayfa / Arkadaşlar) -> Gelen Kutusu",
                preferTabSwitch = true,
                targetTabIds = emptyList(),
                targetHomeTabKeywords = listOf("gelen kutusu", "inbox", "mesajlar", "messages", "profil", "profile")
            )
        }

        return DetectionResult.notDetected(platformName)
    }
}
