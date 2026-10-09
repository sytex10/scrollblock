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

        // 1. KESİN GÜVENLİ ALANLAR (Bu durumlarda ASLA engelleme yapılmaz):
        // Hikayeler (Stories) izleyicisi normal geçici içeriktir, Reels değildir
        val isStoryViewer = NodeUtils.hasViewIdContaining(
            rootNode,
            "reel_viewer",
            "story_viewer",
            "stories_tray"
        )
        if (isStoryViewer && !NodeUtils.hasViewIdContaining(rootNode, "clips_viewer_view_pager")) {
            return DetectionResult.notDetected(platformName)
        }

        // Direkt Mesajlar / Sohbet: Kullanıcı mesajlaşırken engelleme
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

        // Ana Sayfa veya Arama / Profil sekmesi aktifse ve tam ekran Reels izleyicisi yoksa ASLA engelleme
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
        val hasMainFeed = NodeUtils.hasViewIdContaining(
            rootNode,
            "feed_recycler_view",
            "main_feed",
            "newsfeed"
        )
        val isClipsViewPager = NodeUtils.hasViewIdContaining(
            rootNode,
            "clips_viewer_view_pager",
            "clips_viewer_fragment"
        )

        // Eğer kullanıcı ana akışta dolaşıyorsa ve tam ekran Reels açılmamışsa engelleme
        if ((isHomeOrOtherTab || hasMainFeed) && !isClipsViewPager) {
            return DetectionResult.notDetected(platformName)
        }

        // 2. YALNIZCA GERÇEK REELS TESPİTİ:
        // A) Alt bardaki özel Reels sekmesi seçildiyse
        val isReelsTabSelected = NodeUtils.isTabSelected(
            rootNode,
            "clips_tab",
            "reels_tab",
            "reels"
        )
        if (isReelsTabSelected) {
            return DetectionResult.detected(platformName, "Instagram alt bardaki Reels sekmesi seçildi")
        }

        // B) Tam ekran dikey Reels oynatıcı ViewPager'ı (Home feed'de asla bulunmaz)
        if (isClipsViewPager && !hasMainFeed) {
            return DetectionResult.detected(platformName, "Tam ekran Instagram Reels izleyicisi aktif")
        }

        // C) ClipsViewer Activity veya Fragment sınıfı kontrolü
        val className = event?.className?.toString() ?: ""
        if ((className.contains("ClipsViewerActivity", ignoreCase = true) ||
            className.contains("ClipsViewerFragment", ignoreCase = true)) &&
            !hasMainFeed
        ) {
            return DetectionResult.detected(platformName, "Matched ClipsViewer: $className")
        }

        return DetectionResult.notDetected(platformName)
    }
}
