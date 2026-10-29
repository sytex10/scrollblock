package com.example.scrollblock.detector

import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollblock.preferences.BlockPreferences

/**
 * Instagram Reels Engelleyici:
 *  - Alt bardaki Reels sekmesi seçiliyse -> Ana Sayfa sekmesine geçiş (uygulamadan ATMAZ).
 *  - DM'den gelen / dış bağlantıdan açılan tam ekran Reels:
 *      * "DM İzni (Tek Video)" AÇIK ise: İlk video izlenebilir. Kullanıcı bir sonraki videoya
 *        kaydırdığı (scroll) veya video değiştiği anda anında GERİ atılır (DM sohbetine döner).
 *      * "DM İzni" KAPALI ise: Reel açıldığı an doğrudan GERİ atılır.
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

        // ---- 2. TAM EKRAN REELS TESPİTİ (DM / Keşfet / Doğrudan Bağlantı) ----
        val isReelActive = hasVisibleReelsViewer(rootNode, event)

        // ---- 3. GÜVENLİ HİKAYE (STORY) İZLEYİCİSİ ----
        // Sadece ve sadece Reels izleyicisi aktif DEĞİLKEN ve üstte hikaye ilerleme çubuğu varsa hikaye kabul edilir.
        if (!isReelActive && isStoryViewer(rootNode)) {
            singleReelGuard.onViewerGone()
            return DetectionResult.notDetected(platformName)
        }

        if (isReelActive) {
            val isDmReelAllowed = preferences.isInstagramDmReelsAllowedSync()

            // DM İzni (Tek Video) modu KAPALIYSA -> Reel açıldığı an direkt kapat (katı koruma)
            if (!isDmReelAllowed) {
                singleReelGuard.reset()
                return DetectionResult.detected(
                    platform = platformName,
                    reason = "Instagram Reels (katı koruma: DM izni kapalı) -> Geri",
                    preferTabSwitch = false
                )
            }

            // DM İzni (Tek Video) modu AÇIK:
            // a) Kaydırma olayı geldiğinde (kullanıcı parmağıyla sonraki videoya kaydırdı)
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

            // b) Ekrandaki videonun parmak izi değiştiğinde (yazar veya açıklama değişimi)
            val fingerprint = extractVideoFingerprint(rootNode)
            val verdict = singleReelGuard.onViewerVisible(now, fingerprint)
            if (verdict == SingleReelGuard.Verdict.BLOCK) {
                return DetectionResult.detected(
                    platform = platformName,
                    reason = "Instagram DM sonraki videoya geçiş engellendi",
                    preferTabSwitch = false
                )
            }

            // İlk video serbest
            return DetectionResult.notDetected(platformName)
        } else {
            // İzleyici ekranda değilse (DM sohbetine dönüldü vb.) oturumu sıfırla
            singleReelGuard.onViewerGone()
        }

        return DetectionResult.notDetected(platformName)
    }

    /**
     * Instagram'da tam ekran Reels oynatıcısını güvenilir şekilde tespit eder.
     * Hem bilinen view ID'lerini hem de dikey tam ekran ViewPager yapısını inceler.
     */
    private fun hasVisibleReelsViewer(
        rootNode: AccessibilityNodeInfo,
        event: AccessibilityEvent?
    ): Boolean {
        // 1. Bilinen Reels View ID'leri
        val hasClipsId = NodeUtils.hasVisibleViewId(
            rootNode,
            "clips_viewer_view_pager",
            "clips_video_container",
            "clips_viewer_fragment",
            "layout_clips_viewer",
            "clips_media_component",
            "clips_swipe_refresh_container",
            "clips_action_bar_container",
            "clips_author_name"
        )
        if (hasClipsId) return true

        // 2. Pencere sınıf adı kontrolü
        val className = event?.className?.toString() ?: ""
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            (className.contains("ClipsViewer", ignoreCase = true) ||
             className.contains("ReelViewerFragment", ignoreCase = true))
        ) {
            return true
        }

        // 3. Ekranda Reels sağ menü butonları (Yorum, Ses/Audio, Remix vb.) ve video yapısı
        val hasCommentButton = NodeUtils.hasVisibleViewId(rootNode, "comment_button", "row_feed_button_comment")
        val hasAudioOrRemix = NodeUtils.hasVisibleViewId(rootNode, "clips_audio", "remix", "sound_title", "music_title")
        if (hasCommentButton && hasAudioOrRemix) {
            return true
        }

        // 4. Tam ekran dikey ViewPager (ReboundViewPager veya ViewPager2)
        val rootBounds = Rect()
        rootNode.getBoundsInScreen(rootBounds)
        val screenW = rootBounds.width()
        val screenH = rootBounds.height()
        if (screenW <= 0 || screenH <= 0) return false

        val nodeRect = Rect()
        return NodeUtils.anyNodeMatches(rootNode) { node ->
            if (!node.isVisibleToUser) return@anyNodeMatches false
            val cls = node.className?.toString() ?: return@anyNodeMatches false
            if (cls.contains("ViewPager", ignoreCase = true) || cls.contains("ReboundViewPager", ignoreCase = true)) {
                node.getBoundsInScreen(nodeRect)
                nodeRect.width() >= screenW * 0.85f && nodeRect.height() >= screenH * 0.75f
            } else {
                false
            }
        }
    }

    /**
     * Sadece gerçek Instagram Hikaye ekranını tespit eder (Reels ile karıştırmaz).
     */
    private fun isStoryViewer(rootNode: AccessibilityNodeInfo): Boolean {
        val hasStoryProgressBar = NodeUtils.hasVisibleViewId(
            rootNode,
            "story_progress_bar",
            "reel_viewer_progress_bar",
            "indicators_container",
            "progress_bar_wrapper"
        )
        val hasReplyComposer = NodeUtils.hasVisibleViewId(
            rootNode,
            "story_reply_composer",
            "direct_message_edit_text",
            "message_composer_container"
        )
        return hasStoryProgressBar && hasReplyComposer
    }

    /**
     * Ekranda oynatılan aktif videonun kullanıcı adı veya açıklamasından parmak izi üretir.
     */
    private fun extractVideoFingerprint(rootNode: AccessibilityNodeInfo?): String? {
        if (rootNode == null) return null

        val authorNode = NodeUtils.findNode(rootNode) { node ->
            node.isVisibleToUser && (
                node.viewIdResourceName?.endsWith(":id/row_feed_photo_profile_name") == true ||
                node.viewIdResourceName?.contains("clips_author") == true ||
                node.viewIdResourceName?.contains("user_name") == true
            ) && !node.text.isNullOrEmpty()
        }
        val author = authorNode?.text?.toString()?.trim()

        val captionNode = NodeUtils.findNode(rootNode) { node ->
            node.isVisibleToUser && (
                node.viewIdResourceName?.contains("clips_caption") == true ||
                node.viewIdResourceName?.contains("video_caption") == true
            ) && !node.text.isNullOrEmpty()
        }
        val caption = captionNode?.text?.toString()?.trim()

        return when {
            !author.isNullOrEmpty() && !caption.isNullOrEmpty() -> "$author:$caption"
            !author.isNullOrEmpty() -> author
            !caption.isNullOrEmpty() -> caption
            else -> null
        }
    }
}
