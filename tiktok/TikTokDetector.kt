package com.example.scrollblock.detector.tiktok

import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollblock.detector.DetectionResult
import com.example.scrollblock.detector.NodeUtils
import com.example.scrollblock.detector.PlatformDetector
import com.example.scrollblock.preferences.BlockPreferences
import kotlin.math.abs

/**
 * TikTok & Yorum Engelleyici (Tamamen İzole Modül - scrollblock/tiktok):
 *  - Bu klasördeki hiçbir değişiklik Instagram (scrollblock/insta) veya YouTube (scrollblock/yt) modüllerini etkilemez.
 *  - "Yorumları Engelle" AÇIK ise: Yorumlar penceresi açıldığı an 1 kez GERİ basarak kapatır.
 *  - Ana Sayfa (Sizin İçin) video akışı -> Gelen Kutusu sekmesine geçiş (uygulamadan ATMAZ).
 *  - Gelen Kutusu, Sohbet mesaj listesi, Profil, Profil düzenleme, Ayarlar, Hikayeler, Arama -> %100 SERBESTTİR.
 *  - Sohbetten (DM) tıklandığında açılan tam ekran video oynatıcı:
 *      * "DM İzni (Tek Video)" AÇIK ise: İlk video izlenebilir; bir sonraki videoya kaydırılınca GERİ atar.
 *      * "DM İzni" KAPALI ise: Açıldığı an GERİ atarak sohbete döndürür.
 */
class TikTokDetector(
    private val preferences: BlockPreferences,
    private val singleVideoGuard: TikTokSingleVideoGuard = TikTokSingleVideoGuard(),
    private val commentBlocker: TikTokCommentBlocker = TikTokCommentBlocker()
) : PlatformDetector {

    override val platformName: String = "TikTok"
    override val packageNames: Set<String> = setOf(
        "com.zhiliaoapp.musically",
        "com.zhiliaoapp.musically.go",
        "com.ss.android.ugc.trill",
        "com.ss.android.ugc.aweme"
    )

    private val inboxKeywords = listOf("gelen kutusu", "inbox")
    private val homeKeywords = listOf("ana sayfa", "home")
    private val profileKeywords = listOf("profil", "profile")
    private val friendsKeywords = listOf("arkadaşlar", "friends")

    private var inDmChatContext: Boolean = false

    override fun matchesPackage(packageName: String): Boolean {
        val p = packageName.lowercase()
        return p.contains("musically") ||
            p.contains("trill") ||
            p.contains("tiktok") ||
            p.contains("aweme")
    }

    override fun isEnabled(): Boolean =
        preferences.isTikTokEnabledSync() || preferences.isTikTokCommentsBlockedSync()

    override fun detectShortFeed(
        rootNode: AccessibilityNodeInfo?,
        event: AccessibilityEvent?
    ): DetectionResult {
        if (rootNode == null) return DetectionResult.notDetected(platformName)

        // ---- 0. YORUM KALKANI KONTROLÜ (Yorumları Engelle açıksa yorum penceresini kapat) ----
        commentBlocker.evaluate(rootNode, preferences.isTikTokCommentsBlockedSync())?.let { commentResult ->
            return commentResult
        }

        // TikTok akış engelleme kapalıysa (sadece yorum engelleme açıksa) burada dur
        if (!preferences.isTikTokEnabledSync()) {
            inDmChatContext = false
            singleVideoGuard.reset()
            return DetectionResult.notDetected(platformName)
        }

        val now = SystemClock.uptimeMillis()

        // ---- 1. ALT BAR DURUMU ----
        val inboxTab = NodeUtils.findBottomNavTab(rootNode, emptyList(), inboxKeywords)
        val homeTab = NodeUtils.findBottomNavTab(rootNode, emptyList(), homeKeywords)
        val profileTab = NodeUtils.findBottomNavTab(rootNode, emptyList(), profileKeywords)
        val friendsTab = NodeUtils.findBottomNavTab(rootNode, emptyList(), friendsKeywords)

        val inboxSelected = NodeUtils.isNodeSelected(inboxTab)
        val profileSelected = NodeUtils.isNodeSelected(profileTab)
        val homeSelected = NodeUtils.isNodeSelected(homeTab)
        val bottomBarVisible = inboxTab != null || homeTab != null || profileTab != null || friendsTab != null

        val forYouVisibleAtTop = NodeUtils.hasVisibleTextInRegion(
            rootNode, 0f, 0.22f,
            "sizin için", "for you"
        )

        // ---- 2. KESİN GÜVENLİ EKRANLAR (Gelen Kutusu & Kendi Profilimiz) ----
        if ((inboxSelected || profileSelected) && !forYouVisibleAtTop) {
            inDmChatContext = false
            singleVideoGuard.reset()
            return DetectionResult.notDetected(platformName)
        }

        // ---- 3. ANA SAYFA VİDEO AKIŞI -> GELEN KUTUSUNA GEÇ ----
        if (forYouVisibleAtTop || homeSelected) {
            inDmChatContext = false
            singleVideoGuard.reset()
            return DetectionResult.detected(
                platform = platformName,
                reason = "TikTok Ana Sayfa video akışı -> Gelen Kutusu",
                preferTabSwitch = true,
                targetHomeTabKeywords = inboxKeywords
            )
        }

        // ---- 4. BAŞKA BİRİNİN PROFİLİ, HİKAYELER (STORIES), AYARLAR VE ARAMA ----
        if (isUserProfileOrSettings(rootNode) || isStoryOrSearchScreen(rootNode)) {
            inDmChatContext = false
            singleVideoGuard.reset()
            return DetectionResult.notDetected(platformName)
        }

        // ---- 5. SOHBETTEN (DM) AÇILAN TAM EKRAN VİDEO ----
        val hasDmQuickReplyBar = NodeUtils.hasVisibleTextInRegion(
            rootNode, 0.80f, 1f,
            "mesaj gönder", "mesaj yaz", "send a message", "type a message", "message..."
        )
        val isFullScreenVideo = !bottomBarVisible && isFullScreenDmVideoPlayer(rootNode)
        val isPagerScroll = isVerticalPagerScrollEvent(event) && !commentBlocker.isCommentsSheetOpen(rootNode)

        val isDmVideoActive = (inDmChatContext || hasDmQuickReplyBar) && isFullScreenVideo
        val isActiveSessionScroll = singleVideoGuard.isSessionActive &&
            inDmChatContext &&
            !bottomBarVisible &&
            isPagerScroll

        if (isDmVideoActive || isActiveSessionScroll) {
            inDmChatContext = true
            val isDmVideoAllowed = preferences.isTikTokDmVideosAllowedSync()

            if (!isDmVideoAllowed) {
                singleVideoGuard.reset()
                return DetectionResult.detected(
                    platform = platformName,
                    reason = "TikTok DM tam ekran video -> Geri",
                    preferTabSwitch = false
                )
            }

            if (isPagerScroll) {
                val verdict = singleVideoGuard.onPagerScrolled(now)
                if (verdict == TikTokSingleVideoGuard.Verdict.BLOCK) {
                    return DetectionResult.detected(
                        platform = platformName,
                        reason = "TikTok DM video kaydırma engellendi (tek video sınırı)",
                        preferTabSwitch = false
                    )
                }
                return DetectionResult.notDetected(platformName)
            }

            val fingerprint = extractVideoFingerprint(rootNode)
            val verdict = singleVideoGuard.onViewerVisible(now, fingerprint)
            if (verdict == TikTokSingleVideoGuard.Verdict.BLOCK) {
                return DetectionResult.detected(
                    platform = platformName,
                    reason = "TikTok DM sonraki videoya geçiş engellendi",
                    preferTabSwitch = false
                )
            }

            return DetectionResult.notDetected(platformName)
        }

        val isChatConversation = NodeUtils.hasVisibleViewId(
            rootNode,
            "chat_input", "msg_box", "message_edit_text", "et_message", "chat_room"
        ) || hasDmQuickReplyBar

        if (isChatConversation) {
            inDmChatContext = true
            singleVideoGuard.reset()
        } else {
            singleVideoGuard.onViewerGone(now)
        }

        return DetectionResult.notDetected(platformName)
    }

    private fun isUserProfileOrSettings(rootNode: AccessibilityNodeInfo): Boolean {
        val isSettingsOrEdit = NodeUtils.hasVisibleTextInRegion(
            rootNode, 0f, 1f,
            "profili düzenle", "edit profile", "ayarlar ve gizlilik", "settings and privacy",
            "profili paylaş", "share profile"
        )
        if (isSettingsOrEdit) return true

        var profileStatCount = 0
        if (NodeUtils.hasVisibleTextInRegion(rootNode, 0.08f, 0.65f, "takipçi", "followers")) {
            profileStatCount++
        }
        if (NodeUtils.hasVisibleTextInRegion(rootNode, 0.08f, 0.65f, "takip edilen", "following")) {
            profileStatCount++
        }
        if (NodeUtils.hasVisibleTextInRegion(rootNode, 0.08f, 0.65f, "beğeni", "likes")) {
            profileStatCount++
        }
        if (profileStatCount >= 2) return true

        if (hasProfileThumbnailGrid(rootNode)) return true

        return false
    }

    private fun hasProfileThumbnailGrid(rootNode: AccessibilityNodeInfo): Boolean {
        val winRect = Rect()
        rootNode.getBoundsInScreen(winRect)
        val w = winRect.width()
        val h = winRect.height()
        if (w <= 0 || h <= 0) return false

        var gridCellCount = 0
        val rect = Rect()
        NodeUtils.findNode(rootNode) { node ->
            if (node.isVisibleToUser) {
                node.getBoundsInScreen(rect)
                val nw = rect.width()
                val nh = rect.height()
                if (nw in (w * 0.26f).toInt()..(w * 0.36f).toInt() &&
                    nh in (h * 0.12f).toInt()..(h * 0.32f).toInt() &&
                    rect.top >= h * 0.18f
                ) {
                    gridCellCount++
                }
            }
            gridCellCount >= 3
        }
        return gridCellCount >= 3
    }

    private fun isStoryOrSearchScreen(rootNode: AccessibilityNodeInfo): Boolean {
        val hasStoryOrSearchId = NodeUtils.hasVisibleViewId(
            rootNode,
            "search_input", "search_edit_text", "et_search_kw",
            "story_viewer", "story_surface", "story_progress", "story_tag",
            "share_panel", "share_sheet"
        )
        if (hasStoryOrSearchId) return true

        val hasStoryReplyInput = NodeUtils.hasVisibleTextInRegion(
            rootNode, 0.78f, 1f,
            "hikayeye yanıt", "reply to story"
        )
        if (hasStoryReplyInput) return true

        return hasExactStoryBadge(rootNode)
    }

    private fun hasExactStoryBadge(rootNode: AccessibilityNodeInfo): Boolean {
        return NodeUtils.anyNodeMatches(rootNode) { node ->
            if (!node.isVisibleToUser) return@anyNodeMatches false
            val t = node.text?.toString()?.trim() ?: ""
            val d = node.contentDescription?.toString()?.trim() ?: ""
            t.equals("hikaye", ignoreCase = true) ||
                t.equals("story", ignoreCase = true) ||
                t.equals("hikayeler", ignoreCase = true) ||
                t.equals("stories", ignoreCase = true) ||
                d.equals("hikaye", ignoreCase = true) ||
                d.equals("story", ignoreCase = true)
        }
    }

    private fun isFullScreenDmVideoPlayer(rootNode: AccessibilityNodeInfo): Boolean {
        val hasKnownPlayerId = NodeUtils.hasVisibleViewId(
            rootNode,
            "aweme_player", "aweme_vertical_view_pager", "video_post_page"
        ) || NodeUtils.hasViewIdContaining(
            rootNode,
            "aweme_player", "aweme_vertical_view_pager", "video_post_page"
        )
        if (hasKnownPlayerId) return true

        val winRect = Rect()
        rootNode.getBoundsInScreen(winRect)
        val w = winRect.width()
        val h = winRect.height()
        if (w <= 0 || h <= 0) return false

        val rightActionYs = ArrayList<Float>()
        val rightEdgeStripYs = ArrayList<Float>()
        var hasCreatorHandleOrSound = false
        val rect = Rect()

        NodeUtils.findNode(rootNode) { node ->
            if (node.isVisibleToUser) {
                node.getBoundsInScreen(rect)
                val nw = rect.width()
                val nh = rect.height()
                if (nw > 0 && nh > 0) {
                    val cx = rect.exactCenterX()
                    val cy = rect.exactCenterY()
                    val text = node.text?.toString()?.trim() ?: ""
                    val desc = node.contentDescription?.toString()?.trim() ?: ""
                    val combined = "$text $desc".lowercase()

                    if (cx >= w * 0.74f && cy in (h * 0.25f)..(h * 0.86f)) {
                        if (combined.contains("beğen") || combined.contains("like") ||
                            combined.contains("yorum") || combined.contains("comment") ||
                            combined.contains("favori") || combined.contains("favorite") ||
                            combined.contains("kaydet") || combined.contains("bookmark") ||
                            combined.contains("paylaş") || combined.contains("share")
                        ) {
                            rightActionYs.add(cy)
                        }
                        if (cx >= w * 0.78f && nw <= w * 0.22f &&
                            (node.isClickable || text.isNotEmpty() || desc.isNotEmpty())
                        ) {
                            rightEdgeStripYs.add(cy)
                        }
                    }

                    if (cx <= w * 0.72f && cy in (h * 0.48f)..(h * 0.88f)) {
                        if ((text.startsWith("@") && text.length >= 3) ||
                            combined.contains("orijinal ses") ||
                            combined.contains("original sound")
                        ) {
                            hasCreatorHandleOrSound = true
                        }
                    }
                }
            }
            false
        }

        if (rightActionYs.size >= 2) {
            val minY = rightActionYs.minOrNull() ?: 0f
            val maxY = rightActionYs.maxOrNull() ?: 0f
            if (abs(maxY - minY) >= h * 0.035f) return true
        }

        if (rightEdgeStripYs.size >= 3 && hasCreatorHandleOrSound) {
            val minY = rightEdgeStripYs.minOrNull() ?: 0f
            val maxY = rightEdgeStripYs.maxOrNull() ?: 0f
            if (maxY - minY >= h * 0.14f) return true
        }

        return false
    }

    private fun isVerticalPagerScrollEvent(event: AccessibilityEvent?): Boolean {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return false
        val cls = event.className?.toString() ?: ""

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val dx = abs(event.scrollDeltaX)
            val dy = abs(event.scrollDeltaY)
            if (dx > dy && dx > 0) return false
        }

        if (cls.contains("ViewPager", ignoreCase = true)) {
            return true
        }

        if (cls.contains("RecyclerView", ignoreCase = true)) {
            if (event.itemCount >= 2 && event.fromIndex in 0..1 && event.toIndex in 1..2 && event.toIndex > event.fromIndex) {
                return true
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P && abs(event.scrollDeltaY) > 140) {
                return true
            }
        }
        return false
    }

    private fun extractVideoFingerprint(rootNode: AccessibilityNodeInfo?): String? {
        if (rootNode == null) return null

        val winRect = Rect()
        rootNode.getBoundsInScreen(winRect)
        val w = winRect.width()
        val h = winRect.height()
        if (w <= 0 || h <= 0) return null

        val rect = Rect()
        val authorNode = NodeUtils.findNode(rootNode) { node ->
            if (!node.isVisibleToUser) return@findNode false
            val id = node.viewIdResourceName?.lowercase() ?: ""
            val txt = node.text?.toString()?.trim() ?: ""
            if (txt.isEmpty() || txt.contains(" ") || txt.length !in 3..32) return@findNode false

            if (id.contains("title") || id.contains("author") || id.contains("nickname")) {
                return@findNode true
            }
            if (txt.startsWith("@")) {
                node.getBoundsInScreen(rect)
                return@findNode rect.width() > 0 && rect.height() > 0 &&
                    rect.exactCenterX() <= w * 0.70f &&
                    rect.exactCenterY() in (h * 0.45f)..(h * 0.90f)
            }
            false
        }
        return authorNode?.text?.toString()?.trim()
    }
}

