package com.example.scrollblock.detector

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollblock.preferences.BlockPreferences

class YouTubeShortsDetector(
    private val preferences: BlockPreferences
) : PlatformDetector {

    override val platformName: String = "YouTube Shorts"
    override val packageNames: Set<String> = setOf("com.google.android.youtube")

    override fun matchesPackage(packageName: String): Boolean {
        val p = packageName.lowercase()
        return p.contains("youtube")
    }

    override fun isEnabled(): Boolean = preferences.isYouTubeEnabledSync()

    override fun detectShortFeed(
        rootNode: AccessibilityNodeInfo?,
        event: AccessibilityEvent?
    ): DetectionResult {
        if (rootNode == null) {
            return DetectionResult.notDetected(platformName)
        }

        // 1. ÖNCELİKLİ GÜVENLİK KONTROLÜ (Normal uzun video izleme ekranı):
        // Standart video oynatıcı kontrolleri varsa ASLA engelleme.
        // NOT: watch_while_layout gibi tüm uygulamayı kapsayan ana taşıyıcılar buraya konulmamalıdır!
        val hasNormalPlayerControls = NodeUtils.hasViewIdContaining(
            rootNode,
            "time_bar",                     // Video ilerleme çubuğu
            "player_collapse_button",       // Mini oynatıcıya küçültme oku
            "fullscreen_button",            // Tam ekran butonu
            "watch_panel"                   // Normal video altındaki yorum/öneri paneli
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

        // Standart video kontrolleri varsa ve belirgin bir Shorts konteyneri yoksa normal videodur
        if (hasNormalPlayerControls && !hasShortsContainer) {
            return DetectionResult.notDetected(platformName)
        }

        // 2. ALT BARDAKİ SHORTS SEKMESİ SEÇİLDİYSE:
        val isShortsTabSelected = NodeUtils.isTabSelected(rootNode, "shorts")
        if (isShortsTabSelected) {
            return DetectionResult.detected(platformName, "YouTube alt bardaki Shorts sekmesi seçildi")
        }

        // 3. TAM EKRAN SHORTS İZLEYİCİSİ İMZALARI:
        if (hasShortsContainer) {
            return DetectionResult.detected(platformName, "Tam ekran YouTube Shorts oynatıcısı tespit edildi")
        }

        // 4. Shorts'a özel etkileşim öğeleri
        val hasRemixButton = NodeUtils.hasExactViewId(rootNode, "remix_button") ||
            NodeUtils.hasTextOrDescContaining(rootNode, "remix this video", "bu videoyu remiksle")

        val hasShortsSound = NodeUtils.hasExactViewId(rootNode, "sound_button", "reel_sound_pivot_button") ||
            NodeUtils.hasTextOrDescContaining(rootNode, "use this sound", "bu sesi kullan")

        if (hasRemixButton || hasShortsSound) {
            return DetectionResult.detected(platformName, "Shorts Remix / Ses kontrolleri eşleşti")
        }

        // 5. Aktivite sınıfı kontrolü
        val className = event?.className?.toString() ?: ""
        if (className.contains("ReelWatchActivity", ignoreCase = true) ||
            className.contains("ShortsActivity", ignoreCase = true) ||
            className.contains("ReelPlayer", ignoreCase = true)
        ) {
            return DetectionResult.detected(platformName, "Matched Shorts Activity class: $className")
        }

        return DetectionResult.notDetected(platformName)
    }
}
