package com.example.scrollblock.detector

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollblock.preferences.BlockPreferences

class InstagramReelsDetector(
    private val preferences: BlockPreferences
) : PlatformDetector {

    override val platformName: String = "Instagram Reels"
    override val packageNames: Set<String> = setOf("com.instagram.android")

    override fun isEnabled(): Boolean = preferences.isInstagramEnabledSync()

    override fun detectShortFeed(
        rootNode: AccessibilityNodeInfo?,
        event: AccessibilityEvent?
    ): DetectionResult {
        if (rootNode == null) {
            return DetectionResult.notDetected(platformName)
        }

        // 1. KESİN GÜVENLİ ALANLAR (Bu durumlarda ASLA engelleme yapılmaz):
        // Ana sayfa: Üstteki hikayeler tepsisi veya sağ üst DM butonu ekrandaysa normal akıştır.
        val isHomeFeed = NodeUtils.hasViewIdContaining(
            rootNode,
            "stories_tray",             // Ana sayfa hikayeler tepsisi
            "reel_tray",                // Hikaye listesi
            "action_bar_inbox_button",  // Sağ üst DM butonu
            "action_bar_inbox_stub",
            "action_bar_notification_button"
        )
        if (isHomeFeed) {
            return DetectionResult.notDetected(platformName)
        }

        // Direkt Mesajlar / Sohbet: Kullanıcı mesajlaşırken engelleme
        val isDirectMessage = NodeUtils.hasViewIdContaining(
            rootNode,
            "direct_message_thread",
            "message_list",
            "thread_list",
            "message_composer"
        ) || NodeUtils.hasTextOrDescContaining(rootNode, "mesaj gönder", "sohbet")
        if (isDirectMessage) {
            return DetectionResult.notDetected(platformName)
        }

        // 2. YALNIZCA TAM EKRAN DİKEY REELS İZLEYİCİSİ TESPİTİ:
        val isClipsViewPager = NodeUtils.hasViewIdContaining(
            rootNode,
            "clips_viewer_view_pager",
            "clips_viewer_fragment",
            "clips_swipe_refresh_layout"
        )
        if (isClipsViewPager) {
            return DetectionResult.detected(platformName, "Tam ekran Instagram Reels izleyicisi tespit edildi")
        }

        // 3. Aktivite / Fragment kontrolü (Ana sayfa öğeleri yokken)
        val className = event?.className?.toString() ?: ""
        if (className.contains("ClipsViewerActivity", ignoreCase = true) ||
            className.contains("ClipsViewerFragment", ignoreCase = true)
        ) {
            return DetectionResult.detected(platformName, "Matched ClipsViewer: $className")
        }

        // 4. Dikey Reels aksiyon çubuğu ve ses hapı (Yalnızca tam ekrandayken)
        val hasReelsAudio = NodeUtils.hasViewIdContaining(rootNode, "clips_audio_mix_pill")
        val hasReelsActionBar = NodeUtils.hasViewIdContaining(rootNode, "clips_action_bar_container")
        val hasReelsRemix = NodeUtils.hasViewIdContaining(rootNode, "clips_remix_button")

        if ((hasReelsAudio || hasReelsRemix) && hasReelsActionBar) {
            return DetectionResult.detected(platformName, "Dikey Reels aksiyon çubuğu eşleşti")
        }

        return DetectionResult.notDetected(platformName)
    }
}
