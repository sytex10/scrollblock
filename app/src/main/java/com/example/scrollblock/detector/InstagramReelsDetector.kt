package com.example.scrollblock.detector

import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollblock.preferences.BlockPreferences
import kotlin.math.abs

/**
 * Instagram Reels Engelleyici:
 *  - Alt bardaki Reels sekmesi seçiliyse -> Ana Sayfa sekmesine geçiş (uygulamadan ASLA ATMAZ).
 *  - Ana Sayfa akışı (normal kaydırma, gönderi/Reels paylaşma), Hikayeler, DM sohbet ekranı ve Profil -> %100 SERBESTTİR.
 *  - DM'den veya Keşfet'ten tıklandığında açılan tam ekran Reels izleyicisi:
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
        val searchTab = NodeUtils.findBottomNavTab(rootNode, listOf("search_tab"), listOf("ara", "keşfet", "search", "explore"))

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
            singleReelGuard.reset()
            return DetectionResult.notDetected(platformName)
        }

        // Paylaşım (Share) alt penceresi açıksa dokunma
        val isShareSheet = NodeUtils.hasVisibleViewId(
            rootNode,
            "direct_share_sheet",
            "share_sheet"
        )
        if (isShareSheet) {
            return DetectionResult.notDetected(platformName)
        }

        val bottomBarVisible = reelsTab != null || feedTab != null || profileTab != null || searchTab != null

        // ---- 3. TAM EKRAN REELS İZLEYİCİSİ (DM / Keşfet'ten açılan dikey sayfalayıcı) ----
        val isFullScreenReelViewer = isFullScreenClipsViewer(rootNode, event, bottomBarVisible)

        // Aktif bir tek-video oturumu varken gelen dikey sayfalayıcı kaydırmalarını yakala
        val isActiveSessionScroll = singleReelGuard.isSessionActive &&
            !bottomBarVisible &&
            event?.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED &&
            !isCommentsSheetOpen(rootNode)

        if (isFullScreenReelViewer || isActiveSessionScroll) {
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
            if (event?.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED && !isCommentsSheetOpen(rootNode)) {
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
        }

        // Eğer DM sohbet ekranındaysak (arama butonları veya mesaj yazma alanı görünüyorsa) oturumu temizle
        if (isDmChatConversation(rootNode)) {
            singleReelGuard.reset()
        } else {
            singleReelGuard.onViewerGone(now)
        }

        return DetectionResult.notDetected(platformName)
    }

    /**
     * DM'den veya Keşfet'ten açılan tam ekran Reels izleyicisini tespit eder.
     * Hem açık viewId'leri hem de obfuscated (gizlenmiş) Instagram sürümleri için
     * ekranın sağ kenarındaki dikey Reels aksiyon sütununu ve üst "Reels" başlığını kontrol eder.
     */
    private fun isFullScreenClipsViewer(
        rootNode: AccessibilityNodeInfo,
        event: AccessibilityEvent?,
        bottomBarVisible: Boolean
    ): Boolean {
        // 1. Doğrudan Clips izleyici viewId'leri (Görünür alt bileşenler veya ViewPager)
        val hasVisibleClipsId = NodeUtils.hasVisibleViewId(
            rootNode,
            "clips_viewer_view_pager",
            "clips_viewer_fragment",
            "clips_ufi",
            "clips_author",
            "clips_audio",
            "clips_caption",
            "clips_remix"
        ) || (
            NodeUtils.hasViewIdContaining(rootNode, "clips_viewer_view_pager", "clips_viewer_fragment") &&
                NodeUtils.hasVisibleViewId(rootNode, "clips_video_container")
        )
        if (hasVisibleClipsId) return true

        // 2. Pencere sınıf adı (ClipsViewer)
        val className = event?.className?.toString() ?: ""
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            className.contains("ClipsViewer", ignoreCase = true)
        ) {
            return true
        }

        // Hikaye (Story) izleyicisi veya başka bir kullanıcının profil ekranı açıksa Reels değildir
        val isStoryViewer = NodeUtils.hasVisibleViewId(
            rootNode,
            "reel_viewer_media_container",
            "story_viewer"
        ) || NodeUtils.hasVisibleTextInRegion(
            rootNode, 0.80f, 1f,
            "hikayeye yanıt", "reply to story"
        )
        if (isStoryViewer) return false

        val isOtherUserProfile = NodeUtils.hasVisibleTextInRegion(
            rootNode, 0.08f, 0.55f,
            "takipçi", "followers"
        ) && NodeUtils.hasVisibleTextInRegion(
            rootNode, 0.08f, 0.55f,
            "gönderi", "posts", "takip", "following"
        )
        if (isOtherUserProfile) return false

        // DM sohbet üst barındaki sesli/görüntülü arama butonları görünüyorsa henüz videoya tıklanmamıştır
        if (hasDmCallHeader(rootNode)) return false

        // 3. Sağ kenardaki dikey Reels aksiyon sütunu (Beğen + Yorum + Paylaş dikey dizilimi)
        if (hasRightSideReelActions(rootNode)) return true

        // 4. Alt bar gizliyken (DM içindeyken) üst barda "Reels" başlığı veya altta "Orijinal ses" etiketi
        if (!bottomBarVisible) {
            val hasTopReelsTitle = hasExactTopTitle(rootNode, "reels")
            val hasOriginalAudioLabel = NodeUtils.hasVisibleTextInRegion(
                rootNode, 0.50f, 0.92f,
                "orijinal ses", "original audio"
            )
            if (hasTopReelsTitle || hasOriginalAudioLabel) return true
        }

        return false
    }

    /**
     * Instagram Reels tam ekran izleyicisinde ekranın sağ kenarında (x >= %72)
     * dikey olarak sıralanmış Beğen, Yorum, Paylaş/Gönder butonları bulunur.
     * Ana sayfa akışında bu butonlar sol taraftadır; DM sohbetinde ve Hikayelerde ise Yorum butonu yoktur.
     */
    private fun hasRightSideReelActions(rootNode: AccessibilityNodeInfo): Boolean {
        val winRect = Rect()
        rootNode.getBoundsInScreen(winRect)
        val w = winRect.width()
        val h = winRect.height()
        if (w <= 0 || h <= 0) return false

        var likeY: Float? = null
        var commentY: Float? = null
        var shareOrAudioFound = false
        val rect = Rect()

        NodeUtils.findNode(rootNode) { node ->
            if (node.isVisibleToUser) {
                node.getBoundsInScreen(rect)
                val cx = rect.exactCenterX()
                val cy = rect.exactCenterY()
                if (rect.width() > 0 && rect.height() > 0 &&
                    cx >= w * 0.72f && cy in (h * 0.25f)..(h * 0.90f)
                ) {
                    val text = node.text?.toString()?.lowercase() ?: ""
                    val desc = node.contentDescription?.toString()?.lowercase() ?: ""
                    val combined = "$text $desc"

                    if (likeY == null && (
                            combined.contains("beğen") || combined.contains("like")
                        )
                    ) {
                        likeY = cy
                    }
                    if (commentY == null && (
                            combined.contains("yorum") || combined.contains("comment")
                        )
                    ) {
                        commentY = cy
                    }
                    if (combined.contains("paylaş") || combined.contains("share") ||
                        combined.contains("gönder") || combined.contains("send") ||
                        combined.contains("yeniden paylaş") || combined.contains("repost") ||
                        combined.contains("orijinal ses") || combined.contains("original audio")
                    ) {
                        shareOrAudioFound = true
                    }
                }
            }
            false // tüm ağacı tara
        }

        val lY = likeY
        val cY = commentY
        if (lY != null && cY != null && abs(lY - cY) >= h * 0.035f) {
            return true
        }
        if ((lY != null || cY != null) && shareOrAudioFound) {
            return true
        }
        return false
    }

    private fun hasExactTopTitle(rootNode: AccessibilityNodeInfo, title: String): Boolean {
        val h = NodeUtils.windowHeight(rootNode)
        if (h <= 0) return false
        val rect = Rect()
        return NodeUtils.anyNodeMatches(rootNode) { node ->
            if (!node.isVisibleToUser) return@anyNodeMatches false
            val t = node.text?.toString()?.trim() ?: ""
            val d = node.contentDescription?.toString()?.trim() ?: ""
            if (!t.equals(title, ignoreCase = true) && !d.equals(title, ignoreCase = true)) {
                return@anyNodeMatches false
            }
            node.getBoundsInScreen(rect)
            rect.width() > 0 && rect.height() > 0 && rect.exactCenterY() in (h * 0.02f)..(h * 0.16f)
        }
    }

    private fun hasDmCallHeader(rootNode: AccessibilityNodeInfo): Boolean {
        return NodeUtils.hasVisibleTextInRegion(
            rootNode, 0f, 0.18f,
            "sesli arama", "görüntülü arama", "audio call", "video call", "sesli ara", "görüntülü ara"
        ) || NodeUtils.hasVisibleViewId(
            rootNode,
            "header_call_button", "header_video_call_button", "direct_thread_header"
        )
    }

    private fun isDmChatConversation(rootNode: AccessibilityNodeInfo): Boolean {
        if (hasDmCallHeader(rootNode)) return true
        return NodeUtils.hasVisibleViewId(
            rootNode,
            "row_thread_composer", "direct_thread"
        ) || NodeUtils.hasVisibleTextInRegion(
            rootNode, 0.82f, 1f,
            "sesli mesaj", "voice message", "çıkartma", "sticker"
        )
    }

    private fun isCommentsSheetOpen(rootNode: AccessibilityNodeInfo): Boolean {
        return NodeUtils.hasVisibleViewId(
            rootNode,
            "comment_composer", "layout_comment_thread"
        ) || hasExactTopTitle(rootNode, "yorumlar") || hasExactTopTitle(rootNode, "comments")
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

        val byId = when {
            !author.isNullOrEmpty() && !caption.isNullOrEmpty() -> "$author:$caption"
            !author.isNullOrEmpty() -> author
            !caption.isNullOrEmpty() -> caption
            else -> null
        }
        return byId ?: NodeUtils.extractOverlayFingerprint(rootNode)
    }
}
