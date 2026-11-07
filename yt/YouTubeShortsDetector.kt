package com.example.scrollblock.detector.youtube

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollblock.detector.DetectionResult
import com.example.scrollblock.detector.NodeUtils
import com.example.scrollblock.detector.PlatformDetector
import com.example.scrollblock.preferences.BlockPreferences

/**
 * YouTube Shorts & Yorum Engelleyici (Tamamen İzole Modül - scrollblock/yt):
 *  - Bu klasördeki hiçbir değişiklik Instagram (scrollblock/insta) veya TikTok (scrollblock/tiktok) modüllerini etkilemez.
 *  - "Yorumları Engelle" AÇIK ise: Açılan yorumlar panelini otomatik olarak kapatır (video oynamaya devam eder).
 */
class YouTubeShortsDetector(
    private val preferences: BlockPreferences,
    private val commentBlocker: YouTubeCommentBlocker = YouTubeCommentBlocker()
) : PlatformDetector {

    override val platformName: String = "YouTube Shorts"
    override val packageNames: Set<String> = setOf(
        "com.google.android.youtube",
        "app.revanced.android.youtube",
        "com.vanced.android.youtube"
    )

    override fun matchesPackage(packageName: String): Boolean {
        val p = packageName.lowercase()
        // YouTube Music, YouTube Studio, YouTube Kids ve YouTube TV kesinlikle hariç tutulur
        if (p.contains("music") ||
            p.contains("creator") ||
            p.contains("studio") ||
            p.contains("kids") ||
            p.contains("tv") ||
            p.contains("unplugged")
        ) {
            return false
        }
        return packageNames.contains(p) || p == "com.google.android.youtube"
    }

    override fun isEnabled(): Boolean =
        preferences.isYouTubeEnabledSync() || preferences.isYouTubeCommentsBlockedSync()

    override fun detectShortFeed(
        rootNode: AccessibilityNodeInfo?,
        event: AccessibilityEvent?
    ): DetectionResult {
        if (rootNode == null) {
            return DetectionResult.notDetected(platformName)
        }

        // ---- 0. YORUM KALKANI KONTROLÜ (Uzun videolarda ve Shorts'ta yorum panelini kapatır) ----
        commentBlocker.evaluate(rootNode, preferences.isYouTubeCommentsBlockedSync())?.let { commentResult ->
            return commentResult
        }

        // Shorts engelleme kapalıysa (sadece yorum engelleme açıksa) burada dur
        if (!preferences.isYouTubeEnabledSync()) {
            return DetectionResult.notDetected(platformName)
        }

        // 1. ÖNCELİKLİ GÜVENLİK KONTROLÜ (Normal uzun video izleme ekranı):
        val hasNormalPlayerControls = NodeUtils.hasViewIdContaining(
            rootNode,
            "time_bar",
            "player_collapse_button",
            "fullscreen_button",
            "watch_panel"
        )

        val hasShortsContainer = NodeUtils.hasViewIdContaining(
            rootNode,
            "reel_recycler",
            "shorts_container",
            "reel_player_page_tree",
            "shorts_player_surface",
            "reel_player_view",
            "reel_modern_action_bar",
            "shorts_pivot_button",
            "reel_sound_pivot_button"
        )

        if (hasNormalPlayerControls && !hasShortsContainer) {
            return DetectionResult.notDetected(platformName)
        }

        // 2. ALT BARDAKİ SHORTS SEKMESİ SEÇİLDİYSE -> Ana Sayfa sekmesine geç
        val shortsTab = NodeUtils.findBottomNavTab(rootNode, emptyList(), listOf("shorts"))
        if (NodeUtils.isNodeSelected(shortsTab)) {
            return DetectionResult.detected(
                platform = platformName,
                reason = "YouTube alt bardaki Shorts sekmesi -> Ana Sayfa",
                preferTabSwitch = true,
                targetHomeTabKeywords = listOf("ana sayfa", "home")
            )
        }

        // 3. TAM EKRAN SHORTS İZLEYİCİSİ İMZALARI -> Geri çık
        if (hasShortsContainer) {
            return DetectionResult.detected(
                platform = platformName,
                reason = "Tam ekran YouTube Shorts oynatıcısı tespit edildi",
                preferTabSwitch = false
            )
        }

        // 4. Shorts'a özel etkileşim öğeleri
        val hasRemixButton = NodeUtils.hasExactViewId(rootNode, "remix_button") ||
            NodeUtils.hasTextOrDescContaining(rootNode, "remix this video", "bu videoyu remiksle")

        val hasShortsSound = NodeUtils.hasExactViewId(rootNode, "sound_button", "reel_sound_pivot_button") ||
            NodeUtils.hasTextOrDescContaining(rootNode, "use this sound", "bu sesi kullan")

        if (hasRemixButton || hasShortsSound) {
            return DetectionResult.detected(
                platform = platformName,
                reason = "Shorts Remix / Ses kontrolleri eşleşti",
                preferTabSwitch = false
            )
        }

        // 5. Aktivite sınıfı kontrolü
        val className = event?.className?.toString() ?: ""
        if (className.contains("ReelWatchActivity", ignoreCase = true) ||
            className.contains("ShortsActivity", ignoreCase = true) ||
            className.contains("ReelPlayer", ignoreCase = true)
        ) {
            return DetectionResult.detected(
                platform = platformName,
                reason = "Matched Shorts Activity class: $className",
                preferTabSwitch = false
            )
        }

        return DetectionResult.notDetected(platformName)
    }
}

