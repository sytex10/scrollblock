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

        // 1. TAM EKRAN DİKEY REELS İZLEYİCİSİ TESPİTİ (DM veya sohbet içinden tıklananlar dahil):
        val isClipsViewPager = NodeUtils.hasViewIdContaining(
            rootNode,
            "clips_viewer_view_pager",
            "clips_viewer_fragment"
        )
        val className = event?.className?.toString() ?: ""
        val isClipsActivity = className.contains("ClipsViewerActivity", ignoreCase = true) ||
            className.contains("ClipsViewerFragment", ignoreCase = true)

        val hasMainFeed = NodeUtils.hasViewIdContaining(
            rootNode,
            "feed_recycler_view",
            "main_feed",
            "newsfeed"
        )

        // Eğer kullanıcı bir Reel açtıysa (arkadaşın DM'den attığı video dahil), anında kapat!
        if ((isClipsViewPager || isClipsActivity) && !hasMainFeed) {
            return DetectionResult.detected(
                platform = platformName,
                reason = "Instagram Reel izleyicisi aktif (DM/arkadaş videosu veya keşfet)",
                preferTabSwitch = false
            )
        }

        // 2. KESİN GÜVENLİ ALANLAR (Reel izlemiyorken):
        // Hikayeler (Stories) izleyicisi normal geçici içeriktir
        val isStoryViewer = NodeUtils.hasViewIdContaining(
            rootNode,
            "reel_viewer",
            "story_viewer",
            "stories_tray"
        )
        if (isStoryViewer) {
            return DetectionResult.notDetected(platformName)
        }

        // Direkt Mesajlar / Sohbet: Kullanıcı mesajlaşırken ve yazışırken izin ver
        val isDirectMessage = NodeUtils.hasViewIdContaining(
            rootNode,
            "direct_message_thread",
            "message_list",
            "thread_list",
            "message_composer",
            "direct_thread"
        ) || NodeUtils.hasTextOrDescContaining(rootNode, "mesaj gönder", "sohbet")
        if (isDirectMessage) {
            return DetectionResult.notDetected(platformName)
        }

        // Ana Sayfa veya Arama / Profil sekmesi aktifse engelleme
        val isHomeOrOtherTab = NodeUtils.isTabSelected(
            rootNode,
            "feed_tab",
            "search_tab",
            "profile_tab",
            "news_tab",
            "ana sayfa",
            "home",
            "ara",
            "arama",
            "profil"
        )

        if ((isHomeOrOtherTab || hasMainFeed) && !isClipsViewPager) {
            return DetectionResult.notDetected(platformName)
        }

        // 3. ALT BARDAKİ ÖZEL REELS SEKMESİ SEÇİLDİYSE -> Ana Sayfa sekmesine tıkla
        val isReelsTabSelected = NodeUtils.isTabSelected(
            rootNode,
            "clips_tab",
            "reels_tab",
            "reels"
        )
        if (isReelsTabSelected) {
            return DetectionResult.detected(
                platform = platformName,
                reason = "Instagram alt bardaki Reels sekmesi seçildi",
                preferTabSwitch = true,
                targetHomeTabKeywords = listOf("feed_tab", "ana sayfa", "home", "akış")
            )
        }

        return DetectionResult.notDetected(platformName)
    }
}
