package com.example.scrollblock.detector

import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollblock.preferences.BlockPreferences

/**
 * Instagram Reels Engelleyici:
 *  - Alt bardaki Reels sekmesi seçiliyse -> Ana Sayfa sekmesine geçiş (uygulamadan ATMAZ).
 *  - DM'den gelen / dış bağlantıdan açılan Reels:
 *      * "DM İzni (Tek Video)" AÇIK ise: İlk video izlenebilir. Kullanıcı bir sonraki videoya
 *        kaydırdığı (scroll) veya video değiştiği anda anında GERİ atılır.
 *      * "DM İzni" KAPALI ise: Reel açıldığı an kaynak fark etmeksizin kapatılır.
 *  - Ana Sayfa akışı, Hikayeler, DM sohbeti, Profil ve Arama serbesttir.
 */
class InstagramReelsDetector(
    private val preferences: BlockPreferences,
    private val singleReelGuard: SingleReelGuard = SingleReelGuard()
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

        val now = SystemClock.uptimeMillis()

        // ---- 1. ALT BAR REELS SEKMESİ (Her zaman Ana Sayfa'ya yönlendirilir) ----
        val reelsTab = NodeUtils.findBottomNavTab(rootNode, listOf("clips_tab"), listOf("reels"))
        if (NodeUtils.isNodeSelected(reelsTab)) {
            singleReelGuard.reset()
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
        if (storyVisible) {
            singleReelGuard.onViewerGone()
            return DetectionResult.notDetected(platformName)
        }

        // ---- 3. TAM EKRAN REEL İZLEYİCİSİ (DM / Keşfet / Gönderi) ----
        val clipsVisible = NodeUtils.hasVisibleViewId(
            rootNode,
            "clips_viewer_view_pager", "clips_viewer_fragment", "clips_video_container"
        )
        val className = event?.className?.toString() ?: ""
        val clipsActivity = event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            className.contains("ClipsViewer", ignoreCase = true)

        val isReelActive = clipsVisible || clipsActivity

        if (isReelActive) {
            val isDmReelAllowed = preferences.isInstagramDmReelsAllowedSync()

            if (!isDmReelAllowed) {
                singleReelGuard.reset()
                return DetectionResult.detected(
                    platform = platformName,
                    reason = "Instagram tam ekran Reel (katı mod) -> Geri",
                    preferTabSwitch = false
                )
            }

            // DM İzni (Tek Video) modu aktif:
            if (event?.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
                val verdict = singleReelGuard.onPagerScrolled(now)
                if (verdict == SingleReelGuard.Verdict.BLOCK) {
                    return DetectionResult.detected(
                        platform = platformName,
                        reason = "Instagram DM Reels kaydırma engellendi (tek video sınırı)",
                        preferTabSwitch = false
                    )
                }
                return DetectionResult.notDetected(platformName)
            }

            val fingerprint = extractVideoFingerprint(rootNode)
            val verdict = singleReelGuard.onViewerVisible(now, fingerprint)
            if (verdict == SingleReelGuard.Verdict.BLOCK) {
                return DetectionResult.detected(
                    platform = platformName,
                    reason = "Instagram DM Reels sonraki videoya geçiş engellendi",
                    preferTabSwitch = false
                )
            }

            // İlk video serbest bırakıldı
            return DetectionResult.notDetected(platformName)
        } else {
            // İzleyici ekranda değilse oturumu sıfırla
            singleReelGuard.onViewerGone()
        }

        return DetectionResult.notDetected(platformName)
    }

    /**
     * Ekranda görünen aktif videonun kullanıcı adı veya açıklamasını alır.
     * Bu bilgi video değiştiğinde parmak izi farkıyla bir sonraki videoya geçildiğini anlar.
     */
    private fun extractVideoFingerprint(rootNode: AccessibilityNodeInfo?): String? {
        if (rootNode == null) return null
        val author = NodeUtils.findNode(rootNode) { node ->
            node.isVisibleToUser && (
                node.viewIdResourceName?.endsWith(":id/row_feed_photo_profile_name") == true ||
                node.viewIdResourceName?.contains("clips_author") == true ||
                node.viewIdResourceName?.contains("user_name") == true
            )
        }?.text?.toString()

        val caption = NodeUtils.findNode(rootNode) { node ->
            node.isVisibleToUser && (
                node.viewIdResourceName?.contains("clips_caption") == true ||
                node.viewIdResourceName?.contains("video_caption") == true
            )
        }?.text?.toString()

        return when {
            author != null && caption != null -> "$author:$caption"
            author != null -> author
            caption != null -> caption
            else -> null
        }
    }
}
