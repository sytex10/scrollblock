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
 *  - Keşfet'ten (Explore) tıklandığında açılan Reels videoları -> İLK VİDEO DAHİL ANINDA GERİ ATAR (oynatmaz).
 *  - Yalnızca DM sohbetinden tıklandığında açılan tam ekran Reels izleyicisi:
 *      * "DM İzni (Tek Video)" AÇIK ise: İlk video izlenebilir; bir sonraki videoya kaydırılınca GERİ atar.
 *      * "DM İzni" KAPALI ise: Açıldığı an GERİ atarak sohbete döndürür.
 */
class InstagramReelsDetector(
    private val preferences: BlockPreferences,
    private val singleReelGuard: SingleReelGuard = SingleReelGuard()
) : PlatformDetector {

    override val platformName: String = "Instagram Reels"
    override val packageNames: Set<String> = setOf("com.instagram.android")

    /**
     * Kullanıcının bir DM sohbetinden (Direct Message) gelip gelmediğini takip eder.
     * Keşfet (Explore) sekmesinden açılan Reels videolarında bu değer false olur ve ilk video bile oynatılmaz.
     */
    private var inDmChatContext: Boolean = false
    private var inExploreContext: Boolean = false

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
            inDmChatContext = false
            inExploreContext = false
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
            inDmChatContext = false
            inExploreContext = false
            singleReelGuard.reset()
            return DetectionResult.notDetected(platformName)
        }

        val searchTabSelected = NodeUtils.isNodeSelected(searchTab)
        if (searchTabSelected) {
            // Keşfet (Explore) sekmesindeyiz -> DM bağlamını kapat, Keşfet bağlamını aç
            inDmChatContext = false
            inExploreContext = true
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

        // ---- 3. TAM EKRAN REELS / KEŞFET VİDEO İZLEYİCİSİ ----
        val isFullScreenReelViewer = isFullScreenClipsViewer(
            rootNode = rootNode,
            event = event,
            bottomBarVisible = bottomBarVisible,
            searchTabSelected = searchTabSelected || inExploreContext
        )
        val isPagerScroll = isVerticalPagerScrollEvent(event) && !isCommentsSheetOpen(rootNode)

        // Aktif bir tek-video oturumu varken gelen dikey sayfalayıcı kaydırmalarını yakala
        val isActiveSessionScroll = singleReelGuard.isSessionActive &&
            inDmChatContext &&
            !bottomBarVisible &&
            isPagerScroll

        if (isFullScreenReelViewer || isActiveSessionScroll) {
            val hasDmQuickReply = !bottomBarVisible &&
                !searchTabSelected &&
                !inExploreContext &&
                NodeUtils.hasVisibleTextInRegion(
                    rootNode, 0.82f, 1f,
                    "mesaj gönder", "mesaj yaz", "send a message", "message..."
                )
            val hasExploreHeader = hasExactTopTitle(rootNode, "keşfet") || hasExactTopTitle(rootNode, "explore")

            // Eğer video DM sohbetinden DEĞİL (örneğin Keşfet / Explore'dan) açıldıysa:
            // İlk videoya dahi izin verme, anında GERİ tuşuyla Keşfet'e döndür!
            val openedFromDm = (inDmChatContext || hasDmQuickReply) &&
                !searchTabSelected &&
                !inExploreContext &&
                !hasExploreHeader
            if (!openedFromDm) {
                singleReelGuard.reset()
                return DetectionResult.detected(
                    platform = platformName,
                    reason = "Instagram Keşfet Reel -> Geri",
                    preferTabSwitch = false
                )
            }

            inDmChatContext = true
            val isDmReelAllowed = preferences.isInstagramDmReelsAllowedSync()

            if (!isDmReelAllowed) {
                singleReelGuard.reset()
                return DetectionResult.detected(
                    platform = platformName,
                    reason = "Instagram DM tam ekran Reel -> Geri",
                    preferTabSwitch = false
                )
            }

            // DM İzni (Tek Video) modu AÇIK ve video DM'den açıldı:
            if (isPagerScroll) {
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
                    reason = "Instagram DM sonraki videoya geçiş engellendi",
                    preferTabSwitch = false
                )
            }

            return DetectionResult.notDetected(platformName)
        }

        // Eğer DM sohbet ekranındaysak (arama butonları veya mesaj yazma alanı görünüyorsa) DM bağlamını aktif et ve oturumu temizle
        if (isDmChatConversation(rootNode)) {
            inDmChatContext = true
            inExploreContext = false
            singleReelGuard.reset()
        } else {
            singleReelGuard.onViewerGone(now)
        }

        return DetectionResult.notDetected(platformName)
    }

    /**
     * DM'den veya Keşfet'ten açılan tam ekran Reels / video izleyicisini tespit eder.
     * Hem açık viewId'leri hem de obfuscated (gizlenmiş) Instagram sürümleri için
     * ekranın sağ kenarındaki dikey Reels aksiyon sütununu, üst "Reels"/"Keşfet" başlığını
     * ve Keşfet içinde açılan video/gönderi oynatıcısını kontrol eder.
     */
    private fun isFullScreenClipsViewer(
        rootNode: AccessibilityNodeInfo,
        event: AccessibilityEvent?,
        bottomBarVisible: Boolean,
        searchTabSelected: Boolean
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

        // 4. Alt bar gizliyken (DM/Reels içindeyken) veya Keşfet bağlamındayken:
        //    - Üst barda "Reels" veya "< Keşfet" ("Explore") başlığı
        //    - Veya altta "Orijinal ses" / "Original audio" etiketi
        //    - Veya Keşfet'te ızgaradaki bir videoya tıklandığında açılan video/gönderi oynatıcısı
        if (!bottomBarVisible || searchTabSelected) {
            val hasTopReelsOrExploreTitle = hasExactTopTitle(rootNode, "reels") ||
                hasExactTopTitle(rootNode, "keşfet") ||
                hasExactTopTitle(rootNode, "explore")
            val hasOriginalAudioLabel = NodeUtils.hasVisibleTextInRegion(
                rootNode, 0.45f, 0.94f,
                "orijinal ses", "original audio"
            )
            if (hasTopReelsOrExploreTitle || hasOriginalAudioLabel) return true

            if (searchTabSelected && hasExploreOpenedVideoOrPost(rootNode)) {
                return true
            }
        }

        return false
    }

    /**
     * Keşfet (Explore) sekmesinde 3'lü küçük resim ızgarasından bir videoya/Reels'a tıklandığında
     * açılan oynatıcıyı veya Keşfet akışını tespit eder.
     * Ana Keşfet ızgarasında veya arama çubuğunda Beğen + Yorum butonları ya da clips_video_container bulunmaz.
     */
    private fun hasExploreOpenedVideoOrPost(rootNode: AccessibilityNodeInfo): Boolean {
        if (NodeUtils.hasVisibleViewId(
                rootNode,
                "clips_video_container",
                "feed_video_container",
                "row_feed_button_like",
                "row_feed_button_comment"
            )
        ) {
            return true
        }

        var hasLikeButton = false
        var hasCommentButton = false
        val h = NodeUtils.windowHeight(rootNode)
        val rect = Rect()

        NodeUtils.findNode(rootNode) { node ->
            if (node.isVisibleToUser) {
                node.getBoundsInScreen(rect)
                val cy = rect.exactCenterY()
                if (rect.width() > 0 && rect.height() > 0 && (h <= 0 || cy in (h * 0.18f)..(h * 0.90f))) {
                    val t = node.text?.toString()?.trim()?.lowercase() ?: ""
                    val d = node.contentDescription?.toString()?.trim()?.lowercase() ?: ""
                    if (t == "beğen" || t == "like" || d == "beğen" || d == "like" ||
                        d.startsWith("beğen") || d.startsWith("like")
                    ) {
                        hasLikeButton = true
                    }
                    if (t == "yorum yap" || t == "yorum" || t == "comment" ||
                        d.contains("yorum") || d.contains("comment")
                    ) {
                        hasCommentButton = true
                    }
                }
            }
            hasLikeButton && hasCommentButton
        }

        return hasLikeButton && hasCommentButton
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

    /**
     * Yalnızca tam ekran dikey video sayfalayıcısından (ViewPager / ViewPager2 / dikey RecyclerView)
     * gelen gerçek sayfa geçiş kaydırmalarını kabul eder.
     * Video süre çubuğu (SeekBar/ProgressBar) veya kayan müzik adı (TextView) olaylarını yok sayar.
     */
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

    /**
     * Yalnızca videonun SABİT içerik üreticisi (kullanıcı adı / yazar) bilgisini döndürür.
     * Altyazı, süre sayacı (0:14 -> 0:13) veya beğeni sayısı gibi video oynarken değişen metinleri
     * DAHİL ETMEZ; böylece ilk video oynarken asla yanlışlıkla engelleme tetiklenmez.
     */
    private fun extractVideoFingerprint(rootNode: AccessibilityNodeInfo?): String? {
        if (rootNode == null) return null

        val authorById = NodeUtils.findNode(rootNode) { node ->
            node.isVisibleToUser && (
                node.viewIdResourceName?.endsWith(":id/row_feed_photo_profile_name") == true ||
                    node.viewIdResourceName?.contains("clips_author") == true ||
                    node.viewIdResourceName?.contains("user_name") == true
                ) && !node.text.isNullOrEmpty()
        }?.text?.toString()?.trim()

        if (!authorById.isNullOrEmpty()) return authorById

        // Obfuscated sürümler için: Sol-alt yazar bölgesindeki tek kelimelik @kullanici_adi düğümü
        val winRect = Rect()
        rootNode.getBoundsInScreen(winRect)
        val w = winRect.width()
        val h = winRect.height()
        if (w <= 0 || h <= 0) return null

        val ignoredWords = setOf(
            "takip", "follow", "takipet", "following", "reels", "beğen", "like",
            "yorum", "comment", "paylaş", "share", "daha", "more", "ses", "audio",
            "orijinal", "original", "mesaj", "message", "gönder", "send", "sponsorlu", "sponsored"
        )
        val rect = Rect()
        val handleNode = NodeUtils.findNode(rootNode) { node ->
            if (!node.isVisibleToUser) return@findNode false
            val txt = node.text?.toString()?.trim() ?: return@findNode false
            if (txt.length !in 3..30 || txt.contains(" ")) return@findNode false
            val clean = txt.removePrefix("@")
            val isUsernameShape = clean.all { it.isLetterOrDigit() || it == '_' || it == '.' } &&
                clean.any { it.isLetter() }
            if (!isUsernameShape) return@findNode false
            if (ignoredWords.contains(clean.lowercase())) return@findNode false

            node.getBoundsInScreen(rect)
            rect.width() > 0 && rect.height() > 0 &&
                rect.exactCenterX() <= w * 0.62f &&
                rect.exactCenterY() in (h * 0.58f)..(h * 0.90f)
        }

        return handleNode?.text?.toString()?.trim()
    }
}
