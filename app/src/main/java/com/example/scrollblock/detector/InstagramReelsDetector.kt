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

        // 1. KESİN GÜVENLİ ALANLAR - ÖNCELİKLİ KONTROL:
        // A) Ana Sayfa Akışı (Home Feed): Kullanıcı ana sayfada kaydırırken ASLA engelleme / başa atma
        val hasMainFeed = NodeUtils.hasViewIdContaining(
            rootNode,
            "feed_recycler_view",
            "main_feed",
            "newsfeed",
            "feed_carousel",
            "row_feed_photo_imageview",
            "row_feed_view_group"
        )
        if (hasMainFeed) {
            return DetectionResult.notDetected(platformName)
        }

        // B) Hikayeler (Stories) izleyicisi
        val isStoryViewer = NodeUtils.hasViewIdContaining(
            rootNode,
            "reel_viewer",
            "story_viewer",
            "stories_tray",
            "story_progress_bar",
            "reel_viewer_tall_media_container"
        )
        if (isStoryViewer) {
            return DetectionResult.notDetected(platformName)
        }

        // C) Direkt Mesajlar / Sohbet (DM):
        val isDirectMessage = NodeUtils.hasViewIdContaining(
            rootNode,
            "direct_message_thread",
            "message_list",
            "thread_list",
            "message_composer",
            "direct_thread",
            "direct_chat",
            "row_thread",
            "action_bar_title"
        ) || NodeUtils.hasTextOrDescContaining(rootNode, "mesaj gönder", "sohbet", "mesaj yaz", "send message")
        if (isDirectMessage) {
            return DetectionResult.notDetected(platformName)
        }

        // D) Arama / Keşfet Izgarası / Profil / Bildirimler:
        val isOtherSafeTab = NodeUtils.hasViewIdContaining(
            rootNode,
            "action_bar_search_edit_text",
            "search_tab",
            "profile_tab",
            "news_tab",
            "profile_header",
            "row_profile_header"
        )
        if (isOtherSafeTab) {
            return DetectionResult.notDetected(platformName)
        }

        // 2. TAM EKRAN DİKEY REELS İZLEYİCİSİ TESPİTİ (DM veya arama içinden açılanlar dahil):
        val isClipsViewPager = NodeUtils.hasViewIdContaining(
            rootNode,
            "clips_viewer_view_pager",
            "clips_viewer_fragment",
            "clips_video_container"
        )
        val className = event?.className?.toString() ?: ""
        val isClipsActivity = className.contains("ClipsViewerActivity", ignoreCase = true) ||
            className.contains("ClipsViewerFragment", ignoreCase = true)

        if (isClipsViewPager || isClipsActivity) {
            return DetectionResult.detected(
                platform = platformName,
                reason = "Instagram Reel izleyicisi aktif",
                preferTabSwitch = false
            )
        }

        return DetectionResult.notDetected(platformName)
    }
}

