package com.example.scrollblock.detector

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollblock.preferences.BlockPreferences

/**
 * Instagram:
 *  - Alt bardaki Reels sekmesi seçiliyse  -> Ana Sayfa sekmesine geçiş (uygulamadan ATMAZ)
 *  - DM / Keşfet / gönderiden açılan tam ekran Reel -> GERİ (geldiği yere döner)
 *  - Ana Sayfa akışı, Hikayeler, DM, Profil, Arama -> ASLA dokunulmaz
 *
 * Önemli: Sekme değiştikten sonra bellekte gizli kalan Reels fragmanı artık
 * tetikleyici DEĞİL (sadece görünen düğümler sayılır). Bu, çift GERİ ile
 * uygulamadan atma hatasını giderir.
 */
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
        if (rootNode == null) return DetectionResult.notDetected(platformName)

        // ---- 1. ALT BAR DURUMU ----
        val reelsTab = NodeUtils.findBottomNavTab(rootNode, listOf("clips_tab"), listOf("reels"))

        // Reels sekmesi seçili -> Ana Sayfa sekmesine geç
        if (NodeUtils.isNodeSelected(reelsTab)) {
            return DetectionResult.detected(
                platform = platformName,
                reason = "Instagram Reels sekmesi -> Ana Sayfa",
                preferTabSwitch = true,
                targetTabIds = listOf("feed_tab"),
                targetHomeTabKeywords = listOf("ana sayfa", "home")
            )
        }

        // ---- 2. GÜVENLİ: Hikaye izleyicisi ----
        val storyVisible = NodeUtils.hasVisibleViewId(
            rootNode,
            "reel_viewer", "story_viewer", "reel_viewer_media_container"
        )
        if (storyVisible) return DetectionResult.notDetected(platformName)

        // ---- 3. TAM EKRAN REEL (DM / Keşfet / gönderiden açılan) -> GERİ ----
        val clipsVisible = NodeUtils.hasVisibleViewId(
            rootNode,
            "clips_viewer_view_pager", "clips_viewer_fragment", "clips_video_container"
        )
        val className = event?.className?.toString() ?: ""
        val clipsActivity = event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            className.contains("ClipsViewer", ignoreCase = true)

        if (clipsVisible || clipsActivity) {
            // Alt bar yoksa veya başka sekmedeyken üstte açıldıysa GERİ güvenle önceki ekrana döner
            return DetectionResult.detected(
                platform = platformName,
                reason = "Instagram tam ekran Reel -> Geri",
                preferTabSwitch = false
            )
        }

        // Ana sayfa, DM, profil, arama: dokunma
        return DetectionResult.notDetected(platformName)
    }
}
