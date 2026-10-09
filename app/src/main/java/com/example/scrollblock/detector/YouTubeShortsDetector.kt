package com.example.scrollblock.detector

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollblock.preferences.BlockPreferences

class YouTubeShortsDetector(
    private val preferences: BlockPreferences
) : PlatformDetector {

    override val platformName: String = "YouTube Shorts"
    override val packageNames: Set<String> = setOf("com.google.android.youtube")

    override fun isEnabled(): Boolean = preferences.isYouTubeEnabledSync()

    override fun detectShortFeed(
        rootNode: AccessibilityNodeInfo?,
        event: AccessibilityEvent?
    ): DetectionResult {
        if (rootNode == null) {
            return DetectionResult.notDetected(platformName)
        }

        // 1. ÖNCELİKLİ GÜVENLİK KONTROLÜ (Normal video izleme ve ana sayfa):
        val hasNormalPlayerControls = NodeUtils.hasViewIdContaining(
            rootNode,
            "time_bar",             // Normal video ilerleme çubuğu
            "watch_panel",          // Normal video açıklama ve ilgili videolar paneli
            "watch_while_layout",   // Normal video izleme ekranı
            "player_collapse_button",
            "fullscreen_button"
        )

        // Eğer standart video oynatıcı kontrolleri varsa ASLA engelleme
        if (hasNormalPlayerControls) {
            return DetectionResult.notDetected(platformName)
        }

        // 2. YALNIZCA TAM EKRAN SHORTS İZLEYİCİSİ İMZALARI:
        val hasShortsViewerId = NodeUtils.hasViewIdContaining(
            rootNode,
            "reel_player_page_tree",
            "shorts_player_surface",
            "reel_player_pause_button",
            "reel_player_play_button",
            "reel_modern_action_bar"
        )

        if (hasShortsViewerId) {
            return DetectionResult.detected(platformName, "Tam ekran YouTube Shorts oynatıcısı tespit edildi")
        }

        // 3. Shorts'a özel etkileşim öğeleri (Remix butonu, dikey Shorts ses butonu)
        val hasRemixButton = NodeUtils.hasExactViewId(rootNode, "remix_button") ||
            NodeUtils.hasTextOrDescContaining(rootNode, "remix this video", "bu videoyu remiksle")

        val hasShortsSound = NodeUtils.hasExactViewId(rootNode, "sound_button") ||
            NodeUtils.hasTextOrDescContaining(rootNode, "use this sound", "bu sesi kullan")

        if (hasRemixButton || hasShortsSound) {
            return DetectionResult.detected(platformName, "Shorts Remix / Ses kontrolleri eşleşti")
        }

        // 4. Aktivite sınıfı kontrolü
        val className = event?.className?.toString() ?: ""
        if (className.contains("ReelWatchActivity", ignoreCase = true) ||
            className.contains("ShortsActivity", ignoreCase = true)
        ) {
            return DetectionResult.detected(platformName, "Matched Shorts Activity class: $className")
        }

        return DetectionResult.notDetected(platformName)
    }
}
