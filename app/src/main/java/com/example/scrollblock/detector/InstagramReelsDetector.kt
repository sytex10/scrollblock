package com.example.scrollblock.detector

import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollblock.preferences.BlockPreferences

/**
 * Instagram Reels Engelleyici:
 *  - Alt bardaki Reels sekmesi seçiliyse -> Ana Sayfa sekmesine geçiş (uygulamadan ASLA ATMAZ).
 *  - Ana Sayfa akışı (normal kaydırma, gönderi/Reels paylaşma), Hikayeler, DM sohbet ekranı ve Profil -> %100 SERBESTTİR.
 *  - DM'den tıklandığında açılan tam ekran Reels izleyicisi (clips_viewer_view_pager):
 *      * "DM İzni (Tek Video)" AÇIK ise: İlk video izlenebilir; bir sonraki videoya kaydırılınca GERİ atar.
 *      * "DM İzni" KAPALI ise: Açıldığı an GERİ atarak sohbete döndürür.
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

        // ---- 1. ALT BAR SEKMELERİ ----
        val reelsTab = NodeUtils.findBottomNavTab(rootNode, listOf("clips_tab"), listOf("reels"))
        val feedTab = NodeUtils.findBottomNavTab(rootNode, listOf("feed_tab"), listOf("ana sayfa", "home"))
        val profileTab = NodeUtils.findBottomNavTab(rootNode, listOf("profile_tab"), listOf("profil", "profile"))

        // Alt bardaki Reels sekmesi seçiliyse -> Ana Sayfa sekmesine geç (GERİ basılmaz)
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

        // ---- 2. KESİN GÜVENLİ EKRANLAR ----
        // Ana Sayfa veya Profil sekmesi seçiliyse (Ana sayfada kaydırma, gönderi izleme, paylaşım yapma tamamen serbest)
        if (NodeUtils.isNodeSelected(feedTab) || NodeUtils.isNodeSelected(profileTab)) {
            singleReelGuard.onViewerGone()
            return DetectionResult.notDetected(platformName)
        }

        // Hikaye (Story) izleyicisi veya Paylaşım (Share) alt penceresi açıksa dokunma
        val isStoryOrShareSheet = NodeUtils.hasVisibleViewId(
            rootNode,
            "reel_viewer",
            "story_viewer",
            "reel_viewer_media_container",
            "direct_share_sheet",
            "share_sheet",
            "bottom_sheet_container"
        )
        if (isStoryOrShareSheet) {
            singleReelGuard.onViewerGone()
            return DetectionResult.notDetected(platformName)
        }

        // ---- 3. SADECE TAM EKRAN REELS İZLEYİCİSİ (DM / Keşfet'ten açılan dikey sayfalayıcı) ----
        // Not: "clips_video_container" Ana Sayfa akışındaki normal videolarda da bulunduğu için KULLANILMAZ.
        // Yalnızca tam ekran Reels sayfalayıcısı olan "clips_viewer_view_pager" ve "clips_viewer_fragment" kontrol edilir.
        val clipsPagerVisible = NodeUtils.hasVisibleViewId(
            rootNode,
            "clips_viewer_view_pager",
            "clips_viewer_fragment"
        )
        val className = event?.className?.toString() ?: ""
        val clipsActivity = event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            className.contains("ClipsViewer", ignoreCase = true)

        if (clipsPagerVisible || clipsActivity) {
            val isDmReelAllowed = preferences.isInstagramDmReelsAllowedSync()

            if (!isDmReelAllowed) {
                singleReelGuard.reset()
                return DetectionResult.detected(
                    platform = platformName,
                    reason = "Instagram tam ekran Reel -> Geri",
                    preferTabSwitch = false
                )
            }

            // DM İzni (Tek Video) modu AÇIK:
            if (event?.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
                val movedPastFirstItem = event.fromIndex > 0 || (event.toIndex > 0 && event.itemCount > 1)
                val verdict = if (movedPastFirstItem) {
                    SingleReelGuard.Verdict.BLOCK
                } else {
                    singleReelGuard.onPagerScrolled(now)
                }
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
                    reason = "Instagram DM sonraki videoya geçiş engellendi",
                    preferTabSwitch = false
                )
            }

            return DetectionResult.notDetected(platformName)
        } else {
            singleReelGuard.onViewerGone()
        }

        return DetectionResult.notDetected(platformName)
    }

    private fun extractVideoFingerprint(rootNode: AccessibilityNodeInfo?): String? {
        if (rootNode == null) return null

        val author = NodeUtils.findNode(rootNode) { node ->
            node.isVisibleToUser && (
                node.viewIdResourceName?.endsWith(":id/row_feed_photo_profile_name") == true ||
                node.viewIdResourceName?.contains("clips_author") == true ||
                node.viewIdResourceName?.contains("user_name") == true
            ) && !node.text.isNullOrEmpty()
        }?.text?.toString()?.trim()

        val caption = NodeUtils.findNode(rootNode) { node ->
            node.isVisibleToUser && (
                node.viewIdResourceName?.contains("clips_caption") == true ||
                node.viewIdResourceName?.contains("video_caption") == true
            ) && !node.text.isNullOrEmpty()
        }?.text?.toString()?.trim()

        return when {
            !author.isNullOrEmpty() && !caption.isNullOrEmpty() -> "$author:$caption"
            !author.isNullOrEmpty() -> author
            !caption.isNullOrEmpty() -> caption
            else -> null
        }
    }
}
