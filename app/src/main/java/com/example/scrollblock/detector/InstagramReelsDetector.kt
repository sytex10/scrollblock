package com.example.scrollblock.detector

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollblock.preferences.BlockPreferences

class InstagramReelsDetector(
    private val preferences: BlockPreferences
) : PlatformDetector {

    override val platformName: String = "Instagram Reels"
    override val packageNames: Set<String> = setOf("com.instagram.android")

    override fun matchesPackage(packageName: String): Boolean {
        return packageName.lowercase().contains("instagram")
    }

    override fun isEnabled(): Boolean = preferences.isInstagramEnabledSync()

    override fun detectShortFeed(
        rootNode: AccessibilityNodeInfo?,
        event: AccessibilityEvent?
    ): DetectionResult {
        if (rootNode == null) {
            return DetectionResult.notDetected(platformName)
        }

        // 1. REELS İZLEYİCİSİ İMZALARI:
        val hasClipsViewer = NodeUtils.hasViewIdContaining(
            rootNode,
            "clips_viewer_view_pager",
            "clips_viewer_fragment",
            "clips_video_container"
        )
        val className = event?.className?.toString() ?: ""
        val isClipsActivity = className.contains("ClipsViewerActivity", ignoreCase = true) ||
            className.contains("ClipsViewerFragment", ignoreCase = true)

        // Alt bardaki özel Reels sekmesi seçildiyse:
        val isReelsTabSelected = NodeUtils.isTabSelected(
            rootNode,
            "clips_tab",
            "reels_tab"
        )

        // 2. KESİN GÜVENLİ ALANLAR (Reels aktif değilken ASLA engelleme):
        // A) Hikayeler (Stories) izleyicisi:
        val isStoryViewer = NodeUtils.hasViewIdContaining(
            rootNode,
            "reel_viewer",
            "story_viewer",
            "stories_tray"
        )
        if (isStoryViewer && !hasClipsViewer && !isClipsActivity) {
            return DetectionResult.notDetected(platformName)
        }

        // B) Direkt Mesajlar / Sohbet içi yazışma:
        val isDirectChat = NodeUtils.hasViewIdContaining(
            rootNode,
            "direct_message_thread",
            "message_list",
            "thread_list",
            "message_composer",
            "direct_thread"
        )
        if (isDirectChat && !hasClipsViewer && !isClipsActivity) {
            return DetectionResult.notDetected(platformName)
        }

        // C) Ana Sayfa Akışı (Kullanıcı Ana Sayfa sekmesindeyken tam ekran Reels açılmamışsa):
        val isHomeTabSelected = NodeUtils.isTabSelected(rootNode, "feed_tab", "tab_home")
        if (isHomeTabSelected && !hasClipsViewer && !isClipsActivity) {
            return DetectionResult.notDetected(platformName)
        }

        // D) Profil veya Arama sekmesi aktifse:
        val isProfileOrSearchTab = NodeUtils.isTabSelected(
            rootNode,
            "search_tab",
            "profile_tab",
            "news_tab"
        )
        if (isProfileOrSearchTab && !hasClipsViewer && !isClipsActivity) {
            return DetectionResult.notDetected(platformName)
        }

        // 3. REELS TESPİTİ:
        if (hasClipsViewer || isClipsActivity || isReelsTabSelected) {
            return DetectionResult.detected(
                platform = platformName,
                reason = "Instagram Reel tespit edildi",
                preferTabSwitch = false
            )
        }

        return DetectionResult.notDetected(platformName)
    }
}

