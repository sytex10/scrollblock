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
 *  - Ana Sayfa akışı (alt bar gizlense bile normal kaydırma, gönderi/Reels izleme), Hikayeler,
 *    DM sohbet ekranı, Arama/Keşfet ızgarası ve Profil -> %100 SERBESTTİR ve ASLA GERİ BASMAZ.
 *  - Keşfet'ten (Explore) tıklandığında açılan Reels videoları ->
 *      * Alt bar görünürse Keşfet sekmesine tıklayarak ızgaraya döner (GERİ tuşu kullanmaz, uygulamadan atmaz).
 *      * Tam ekran ise yalnızca 1 kez GERİ basarak Keşfet ızgarasına döner (zincirleme GERİ basmaz).
 *  - DM sohbetinden tıklandığında açılan tam ekran Reels izleyicisi:
 *      * "DM İzni (Tek Video)" AÇIK ise: İlk video izlenebilir; bir sonraki videoya kaydırılınca 1 kez GERİ basar.
 *      * "DM İzni" KAPALI ise: Açıldığı an 1 kez GERİ basarak sohbete döndürür.
 */
class InstagramReelsDetector(
    private val preferences: BlockPreferences,
    private val singleReelGuard: SingleReelGuard = SingleReelGuard()
) : PlatformDetector {

    override val platformName: String = "Instagram Reels"
    override val packageNames: Set<String> = setOf("com.instagram.android")

    private enum class ActiveSection {
        HOME,
        PROFILE,
        EXPLORE,
        DM,
        REELS
    }

    private var activeSection: ActiveSection = ActiveSection.HOME

    /**
     * Tam ekran oynatıcıdan çıkmak için GERİ tuşuna basıldığında true olur.
     * Güvenli bir ekrana (DM sohbeti, Keşfet ızgarası veya alt bar) dönülene kadar
     * ikinci kez GERİ basılmasını engelleyerek uygulamadan atmayı %100 önler.
     */
    private var awaitingOverlayExit: Boolean = false

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
            activeSection = ActiveSection.REELS
            awaitingOverlayExit = false
            singleReelGuard.reset()
            return DetectionResult.detected(
                platform = platformName,
                reason = "Instagram Reels sekmesi -> Ana Sayfa",
                preferTabSwitch = true,
                targetTabIds = listOf("feed_tab"),
                targetHomeTabKeywords = listOf("ana sayfa", "home")
            )
        }

        // ---- 2. KESİN GÜVENLİ EKRANLAR (Ana Sayfa, Profil, Hikaye, Paylaşım, DM Sohbet, Keşfet Izgarası) ----
        if (NodeUtils.isNodeSelected(feedTab) || isHomeFeedScreen(rootNode)) {
            activeSection = ActiveSection.HOME
            awaitingOverlayExit = false
            singleReelGuard.reset()
            return DetectionResult.notDetected(platformName)
        }

        if (NodeUtils.isNodeSelected(profileTab) || isOtherUserProfile(rootNode)) {
            activeSection = ActiveSection.PROFILE
            awaitingOverlayExit = false
            singleReelGuard.reset()
            return DetectionResult.notDetected(platformName)
        }

        if (isStoryViewer(rootNode) || isShareSheet(rootNode)) {
            awaitingOverlayExit = false
            return DetectionResult.notDetected(platformName)
        }

        if (isDmChatConversation(rootNode)) {
            activeSection = ActiveSection.DM
            awaitingOverlayExit = false
            singleReelGuard.reset()
            return DetectionResult.notDetected(platformName)
        }

        val searchTabSelected = NodeUtils.isNodeSelected(searchTab)
        if (searchTabSelected) {
            activeSection = ActiveSection.EXPLORE
            if (isExploreGridOrSearchScreen(rootNode)) {
                awaitingOverlayExit = false
                singleReelGuard.reset()
                return DetectionResult.notDetected(platformName)
            }
        }

        // Eğer kullanıcı Ana Sayfa veya Profil sekmesinde gezinirken alt bar gizlendiyse
        // (ör. Ana Sayfa akışında aşağı doğru kaydırırken) ASLA engelleme yapma!
        if (activeSection == ActiveSection.HOME || activeSection == ActiveSection.PROFILE) {
            val hasDmQuickReplyOnScreen = NodeUtils.hasVisibleTextInRegion(
                rootNode, 0.82f, 1f,
                "mesaj gönder", "mesaj yaz", "send a message", "message..."
            )
            if (!hasDmQuickReplyOnScreen) {
                singleReelGuard.reset()
                return DetectionResult.notDetected(platformName)
            }
        }

        val bottomBarVisible = reelsTab != null || feedTab != null || profileTab != null || searchTab != null

        // ---- 3. TAM EKRAN REELS İZLEYİCİSİ (DM veya Keşfet'ten açılan tam ekran Reels) ----
        val isFullScreenReelViewer = isFullScreenClipsViewer(rootNode, event, bottomBarVisible)
        val isPagerScroll = isVerticalPagerScrollEvent(event) && !isCommentsSheetOpen(rootNode)

        val isActiveSessionScroll = singleReelGuard.isSessionActive &&
            activeSection == ActiveSection.DM &&
            !bottomBarVisible &&
            isPagerScroll

        if (isFullScreenReelViewer || isActiveSessionScroll) {
            val hasDmQuickReply = !bottomBarVisible &&
                !searchTabSelected &&
                NodeUtils.hasVisibleTextInRegion(
                    rootNode, 0.82f, 1f,
                    "mesaj gönder", "mesaj yaz", "send a message", "message..."
                )

            val openedFromDm = (activeSection == ActiveSection.DM || hasDmQuickReply) &&
                !searchTabSelected &&
                activeSection != ActiveSection.EXPLORE

            // A) Keşfet'ten (Explore) açılan Reels videosu -> İlk video dahil oynatma, Keşfet ızgarasına döndür
            if (!openedFromDm) {
                singleReelGuard.reset()
                // Alt bar görünürse GERİ basmak yerine Keşfet sekmesine tıkla (uygulamadan atma ihtimali %0)
                if (bottomBarVisible) {
                    return DetectionResult.detected(
                        platform = platformName,
                        reason = "Instagram Keşfet Reel -> Keşfet ızgarasına dön",
                        preferTabSwitch = true,
                        targetTabIds = listOf("search_tab", "feed_tab"),
                        targetHomeTabKeywords = listOf("ara", "keşfet", "search", "explore", "ana sayfa", "home")
                    )
                }
                // Alt bar gizliyse (tam ekran katman) yalnızca 1 kez GERİ bas
                if (awaitingOverlayExit) {
                    return DetectionResult.notDetected(platformName)
                }
                awaitingOverlayExit = true
                return DetectionResult.detected(
                    platform = platformName,
                    reason = "Instagram Keşfet tam ekran Reel -> Geri",
                    preferTabSwitch = false
                )
            }

            // B) DM sohbetinden açılan Reels videosu
            activeSection = ActiveSection.DM
            val isDmReelAllowed = preferences.isInstagramDmReelsAllowedSync()

            if (!isDmReelAllowed) {
                singleReelGuard.reset()
                if (awaitingOverlayExit) {
                    return DetectionResult.notDetected(platformName)
                }
                awaitingOverlayExit = true
                return DetectionResult.detected(
                    platform = platformName,
                    reason = "Instagram DM tam ekran Reel -> Geri",
                    preferTabSwitch = false
                )
            }

            // DM İzni (Tek Video) AÇIK: İlk videoya izin ver, kaydırınca 1 kez GERİ bas
            if (isPagerScroll) {
                val verdict = singleReelGuard.onPagerScrolled(now)
                if (verdict == SingleReelGuard.Verdict.BLOCK) {
                    if (awaitingOverlayExit) {
                        return DetectionResult.notDetected(platformName)
                    }
                    awaitingOverlayExit = true
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
                if (awaitingOverlayExit) {
                    return DetectionResult.notDetected(platformName)
                }
                awaitingOverlayExit = true
                return DetectionResult.detected(
                    platform = platformName,
                    reason = "Instagram DM sonraki videoya geçiş engellendi",
                    preferTabSwitch = false
                )
            }

            return DetectionResult.notDetected(platformName)
        }

        // Tam ekran oynatıcı kapandıysa kilidi sıfırla
        awaitingOverlayExit = false
        singleReelGuard.onViewerGone(now)
        return DetectionResult.notDetected(platformName)
    }

    /**
     * Yalnızca gerçek tam ekran Reels (ClipsViewer) oynatıcısını tespit eder.
     * Gizli (bellekte kalmış) fragment'ları veya ızgara/akış içi küçük video önizlemelerini ASLA eşleştirmez.
     */
    private fun isFullScreenClipsViewer(
        rootNode: AccessibilityNodeInfo,
        event: AccessibilityEvent?,
        bottomBarVisible: Boolean
    ): Boolean {
        // Keşfet arama çubuğu / 3'lü ızgara görünüyorsa ve sağ kenarda Reels buton sütunu yoksa izleyici değildir
        if (isExploreGridOrSearchScreen(rootNode)) return false

        // 1. Yalnızca GÖRÜNÜR tam ekran ClipsViewPager / ClipsViewerFragment bileşenleri
        val hasVisibleClipsPager = NodeUtils.hasVisibleViewId(
            rootNode,
            "clips_viewer_view_pager",
            "clips_viewer_fragment"
        )
        if (hasVisibleClipsPager) return true

        // 2. Pencere sınıf adı (ClipsViewer)
        val className = event?.className?.toString() ?: ""
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            className.contains("ClipsViewer", ignoreCase = true)
        ) {
            return true
        }

        // 3. Sağ kenardaki dikey Reels aksiyon sütunu (Beğen üstte + Yorum altta + Paylaş/Ses sağ kenarda)
        if (hasRightSideReelActions(rootNode)) return true

        // 4. Alt bar gizliyken üst barda açıkça "Reels" başlığı varsa
        if (!bottomBarVisible && hasExactTopTitle(rootNode, "reels")) {
            return true
        }

        return false
    }

    /**
     * Ana Sayfa (Home Feed) üst barını veya hikaye tepsisini tespit eder.
     */
    private fun isHomeFeedScreen(rootNode: AccessibilityNodeInfo): Boolean {
        return NodeUtils.hasVisibleViewId(
            rootNode,
            "reel_tray",
            "MainFeedFragment",
            "feed_timeline"
        ) || NodeUtils.hasVisibleTextInRegion(
            rootNode, 0f, 0.22f,
            "hikayen", "your story", "senin için", "for you"
        )
    }

    /**
     * Keşfet 3'lü küçük resim ızgarasını veya arama ekranını tespit eder.
     * Sağ kenarda Reels aksiyon sütunu varken (Reels oynatıcısı açıkken) false döner.
     */
    private fun isExploreGridOrSearchScreen(rootNode: AccessibilityNodeInfo): Boolean {
        if (hasRightSideReelActions(rootNode)) return false

        if (NodeUtils.hasVisibleViewId(
                rootNode,
                "action_bar_search_edit_text",
                "search_row"
            )
        ) {
            return true
        }

        // Üst bölgede geniş Arama çubuğu ("Ara" / "Search") kontrolü (sağ üstteki küçük büyüteç ikonunu hariç tutar)
        val winRect = Rect()
        rootNode.getBoundsInScreen(winRect)
        val w = winRect.width()
        val h = winRect.height()
        if (w <= 0 || h <= 0) return false

        val rect = Rect()
        return NodeUtils.anyNodeMatches(rootNode) { node ->
            if (!node.isVisibleToUser) return@anyNodeMatches false
            val t = node.text?.toString()?.trim()?.lowercase() ?: ""
            val d = node.contentDescription?.toString()?.trim()?.lowercase() ?: ""
            if (t != "ara" && t != "search" && d != "ara" && d != "search") {
                return@anyNodeMatches false
            }
            node.getBoundsInScreen(rect)
            rect.width() > 0 && rect.height() > 0 &&
                rect.exactCenterY() in (h * 0.02f)..(h * 0.16f) &&
                (rect.width() >= w * 0.30f || rect.exactCenterX() <= w * 0.65f)
        }
    }

    private fun isStoryViewer(rootNode: AccessibilityNodeInfo): Boolean {
        return NodeUtils.hasVisibleViewId(
            rootNode,
            "reel_viewer_media_container",
            "story_viewer"
        ) || NodeUtils.hasVisibleTextInRegion(
            rootNode, 0.80f, 1f,
            "hikayeye yanıt", "reply to story"
        )
    }

    private fun isOtherUserProfile(rootNode: AccessibilityNodeInfo): Boolean {
        return NodeUtils.hasVisibleTextInRegion(
            rootNode, 0.08f, 0.55f,
            "takipçi", "followers"
        ) && NodeUtils.hasVisibleTextInRegion(
            rootNode, 0.08f, 0.55f,
            "gönderi", "posts", "takip", "following"
        )
    }

    private fun isShareSheet(rootNode: AccessibilityNodeInfo): Boolean {
        return NodeUtils.hasVisibleViewId(
            rootNode,
            "direct_share_sheet",
            "share_sheet"
        )
    }

    /**
     * Instagram Reels tam ekran izleyicisinde ekranın sağ kenarında (left >= %68, width <= %28)
     * dikey olarak aynı sütunda sıralanmış küçük ikon butonları bulunur:
     *  - Üstte Beğen (likeY)
     *  - Onun altında Yorum (commentY > likeY)
     *  - Ve aynı sütunda Paylaş/Gönder/Ses
     * Bu sıkı geometrik kontrol, DM sohbet balonlarını veya Ana Sayfa gönderilerini ASLA eşleştirmez.
     */
    private fun hasRightSideReelActions(rootNode: AccessibilityNodeInfo): Boolean {
        val winRect = Rect()
        rootNode.getBoundsInScreen(winRect)
        val w = winRect.width()
        val h = winRect.height()
        if (w <= 0 || h <= 0) return false

        var likeX: Float? = null
        var likeY: Float? = null
        var commentX: Float? = null
        var commentY: Float? = null
        var shareOrAudioFound = false
        val rect = Rect()

        NodeUtils.findNode(rootNode) { node ->
            if (node.isVisibleToUser) {
                node.getBoundsInScreen(rect)
                val cx = rect.exactCenterX()
                val cy = rect.exactCenterY()
                val isCompactRightEdgeIcon = rect.width() in 1..(w * 0.28f).toInt() &&
                    rect.height() in 1..(h * 0.16f).toInt() &&
                    rect.left >= w * 0.68f &&
                    cx >= w * 0.72f &&
                    cy in (h * 0.28f)..(h * 0.88f)

                if (isCompactRightEdgeIcon) {
                    val text = node.text?.toString()?.lowercase() ?: ""
                    val desc = node.contentDescription?.toString()?.lowercase() ?: ""
                    val combined = "$text $desc"

                    if (likeY == null && (
                            combined.contains("beğen") || combined.contains("like")
                        )
                    ) {
                        likeX = cx
                        likeY = cy
                    }
                    if (commentY == null && (
                            combined.contains("yorum") || combined.contains("comment")
                        )
                    ) {
                        commentX = cx
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

        val lX = likeX ?: return false
        val lY = likeY ?: return false
        val cX = commentX ?: return false
        val cY = commentY ?: return false

        val sameVerticalColumn = abs(lX - cX) <= w * 0.10f
        val commentBelowLike = (cY - lY) in (h * 0.035f)..(h * 0.24f)

        return sameVerticalColumn && commentBelowLike && shareOrAudioFound
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
            "row_thread_composer",
            "direct_thread",
            "thread_title",
            "direct_inbox",
            "inbox_refreshable_thread_list_recyclerview"
        ) || NodeUtils.hasVisibleTextInRegion(
            rootNode, 0.82f, 1f,
            "sesli mesaj", "voice message", "çıkartma", "sticker", "mesaj...", "message..."
        ) || NodeUtils.hasVisibleTextInRegion(
            rootNode, 0f, 0.25f,
            "mesajlar", "messages", "sohbetler", "chats", "istekler", "requests"
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
