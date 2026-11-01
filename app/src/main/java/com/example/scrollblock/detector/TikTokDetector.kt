package com.example.scrollblock.detector

import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollblock.preferences.BlockPreferences
import kotlin.math.abs

/**
 * TikTok Engelleyici:
 *  - Ana Sayfa (Sizin İçin) video akışı -> Gelen Kutusu sekmesine geçiş (uygulamadan ATMAZ).
 *  - Gelen Kutusu, Sohbet mesaj listesi, Profil, Profil düzenleme, Ayarlar, Hikayeler, Arama -> %100 SERBESTTİR.
 *  - Sohbetten (DM) tıklandığında açılan tam ekran video oynatıcı:
 *      * "DM İzni (Tek Video)" AÇIK ise: İlk video izlenebilir; bir sonraki videoya kaydırılınca GERİ atar.
 *      * "DM İzni" KAPALI ise: Açıldığı an GERİ atarak sohbete döndürür.
 */
class TikTokDetector(
    private val preferences: BlockPreferences,
    private val singleVideoGuard: SingleReelGuard = SingleReelGuard()
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

    override fun matchesPackage(packageName: String): Boolean {
        val p = packageName.lowercase()
        return p.contains("musically") ||
            p.contains("trill") ||
            p.contains("tiktok") ||
            p.contains("aweme")
    }

    override fun isEnabled(): Boolean = preferences.isTikTokEnabledSync()

    override fun detectShortFeed(
        rootNode: AccessibilityNodeInfo?,
        event: AccessibilityEvent?
    ): DetectionResult {
        if (rootNode == null) return DetectionResult.notDetected(platformName)

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

        // Ekranın üst kısmında görünen "Sizin İçin" sekmesi = kesin ana video akışı
        val forYouVisibleAtTop = NodeUtils.hasVisibleTextInRegion(
            rootNode, 0f, 0.22f,
            "sizin için", "for you"
        )

        // ---- 2. KESİN GÜVENLİ EKRANLAR (Gelen Kutusu & Profil Sekmeleri) ----
        if ((inboxSelected || profileSelected) && !forYouVisibleAtTop) {
            singleVideoGuard.reset()
            return DetectionResult.notDetected(platformName)
        }

        // Profil düzenleme / ayarlar / arama / hikaye / paylaşım ekranları
        val isEditOrSettings = NodeUtils.hasVisibleTextInRegion(
            rootNode, 0f, 1f,
            "profili düzenle", "edit profile", "ayarlar ve gizlilik", "settings and privacy",
            "kullanıcı adı", "username", "biyografi", "bio"
        )
        val isSearchOrStory = NodeUtils.hasVisibleViewId(
            rootNode,
            "search_input", "search_edit_text", "et_search_kw",
            "story_viewer", "story_surface", "story_progress",
            "share_panel", "share_sheet"
        )
        if (isEditOrSettings || isSearchOrStory) {
            singleVideoGuard.onViewerGone(now)
            return DetectionResult.notDetected(platformName)
        }

        // ---- 3. ANA SAYFA VİDEO AKIŞI -> GELEN KUTUSUNA GEÇ ----
        if (forYouVisibleAtTop || homeSelected) {
            singleVideoGuard.reset()
            return DetectionResult.detected(
                platform = platformName,
                reason = "TikTok Ana Sayfa video akışı -> Gelen Kutusu",
                preferTabSwitch = true,
                targetHomeTabKeywords = inboxKeywords
            )
        }

        // ---- 4. SOHBETTEN (DM) AÇILAN TAM EKRAN VİDEO ----
        // ÖNEMLİ: TikTok DM'den açılan tam ekran videonun en altında da "Mesaj gönder..." çubuğu bulunur.
        // Bu nedenle alt bar gizliyken (!bottomBarVisible) önce tam ekran video oynatıcı kontrol edilir!
        val isFullScreenVideo = !bottomBarVisible && isFullScreenDmVideoPlayer(rootNode)
        val isActiveSessionScroll = singleVideoGuard.isSessionActive &&
            !bottomBarVisible &&
            event?.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED &&
            !isCommentsSheetOpen(rootNode)

        if (isFullScreenVideo || isActiveSessionScroll) {
            val isDmVideoAllowed = preferences.isTikTokDmVideosAllowedSync()

            if (!isDmVideoAllowed) {
                singleVideoGuard.reset()
                return DetectionResult.detected(
                    platform = platformName,
                    reason = "TikTok DM tam ekran video -> Geri",
                    preferTabSwitch = false
                )
            }

            // DM İzni (Tek Video) modu AÇIK:
            if (event?.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED && !isCommentsSheetOpen(rootNode)) {
                val movedPastFirstItem = event.fromIndex > 0 || (event.toIndex > 0 && event.itemCount > 1)
                val verdict = if (movedPastFirstItem) {
                    SingleReelGuard.Verdict.BLOCK
                } else {
                    singleVideoGuard.onPagerScrolled(now)
                }
                if (verdict == SingleReelGuard.Verdict.BLOCK) {
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
            if (verdict == SingleReelGuard.Verdict.BLOCK) {
                return DetectionResult.detected(
                    platform = platformName,
                    reason = "TikTok DM sonraki videoya geçiş engellendi",
                    preferTabSwitch = false
                )
            }

            return DetectionResult.notDetected(platformName)
        }

        // Tam ekran video oynatıcıda değilsek ve normal sohbet ekranındaysak oturumu sıfırla
        val isChatConversation = NodeUtils.hasVisibleViewId(
            rootNode,
            "chat_input", "msg_box", "message_edit_text", "et_message", "chat_room"
        ) || NodeUtils.hasVisibleTextInRegion(
            rootNode, 0.72f, 1f,
            "mesaj gönder", "mesaj yaz", "send a message", "type a message", "message..."
        )
        if (isChatConversation) {
            singleVideoGuard.reset()
        } else {
            singleVideoGuard.onViewerGone(now)
        }

        return DetectionResult.notDetected(platformName)
    }

    /**
     * Alt bar gizliyken (DM içindeyken) tam ekran video oynatıcının açık olup olmadığını tespit eder.
     * Hem bilinen viewId'leri hem de obfuscated TikTok sürümleri için sağ kenar aksiyon sütununu,
     * tam ekran dikey ViewPager'ı ve video üstü katmanlarını kontrol eder.
     */
    private fun isFullScreenDmVideoPlayer(rootNode: AccessibilityNodeInfo): Boolean {
        // 1. Bilinen TikTok video oynatıcı viewId'leri
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

        var hasFullScreenViewPager = false
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
                    val cls = node.className?.toString() ?: ""
                    if (cls.contains("ViewPager", ignoreCase = true) &&
                        nw >= w * 0.85f && nh >= h * 0.70f
                    ) {
                        hasFullScreenViewPager = true
                    }

                    val cx = rect.exactCenterX()
                    val cy = rect.exactCenterY()
                    val text = node.text?.toString()?.trim() ?: ""
                    val desc = node.contentDescription?.toString()?.trim() ?: ""
                    val combined = "$text $desc".lowercase()

                    // Sağ kenar video aksiyon sütunu (Beğen, Yorum, Kaydet/Favori, Paylaş)
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

                    // Sol-alt içerik üreticisi (@kullanici) veya orijinal ses etiketi
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
            false // tüm ağacı tara
        }

        // 2. Alt bar yokken tam ekran ViewPager yalnızca DM'den/profilden açılan tam ekran video oynatıcıda vardır
        // (Normal DM sohbet ekranı RecyclerView kullanır, ViewPager kullanmaz)
        if (hasFullScreenViewPager) return true

        // 3. Sağ kenarda dikey sıralı en az 2 video etkileşim butonu
        if (rightActionYs.size >= 2) {
            val minY = rightActionYs.minOrNull() ?: 0f
            val maxY = rightActionYs.maxOrNull() ?: 0f
            if (abs(maxY - minY) >= h * 0.035f) return true
        }

        // 4. Sağ şeritte dikey dizili en az 3 sayaç/ikon + sol altta @kullanıcı veya ses bilgisi
        if (rightEdgeStripYs.size >= 3 && hasCreatorHandleOrSound) {
            val minY = rightEdgeStripYs.minOrNull() ?: 0f
            val maxY = rightEdgeStripYs.maxOrNull() ?: 0f
            if (maxY - minY >= h * 0.14f) return true
        }

        return false
    }

    private fun isCommentsSheetOpen(rootNode: AccessibilityNodeInfo): Boolean {
        return NodeUtils.hasVisibleViewId(
            rootNode,
            "comment_list", "comment_container"
        ) || NodeUtils.hasVisibleTextInRegion(
            rootNode, 0.20f, 0.45f,
            "yorumlar", "comments"
        )
    }

    private fun extractVideoFingerprint(rootNode: AccessibilityNodeInfo?): String? {
        if (rootNode == null) return null

        val h = NodeUtils.windowHeight(rootNode)
        val rect = Rect()

        val authorNode = NodeUtils.findNode(rootNode) { node ->
            if (!node.isVisibleToUser) return@findNode false
            val id = node.viewIdResourceName?.lowercase() ?: ""
            val txt = node.text?.toString()?.trim() ?: ""
            if (id.contains("title") || id.contains("author") || id.contains("nickname")) {
                return@findNode txt.isNotEmpty()
            }
            if (txt.startsWith("@") && txt.length > 2 && h > 0) {
                node.getBoundsInScreen(rect)
                return@findNode rect.exactCenterY() in (h * 0.45f)..(h * 0.92f)
            }
            false
        }
        val author = authorNode?.text?.toString()?.trim()

        val captionNode = NodeUtils.findNode(rootNode) { node ->
            if (!node.isVisibleToUser) return@findNode false
            val id = node.viewIdResourceName?.lowercase() ?: ""
            val txt = node.text?.toString()?.trim() ?: ""
            (id.contains("desc") || id.contains("caption")) && txt.isNotEmpty()
        }
        val caption = captionNode?.text?.toString()?.trim()

        val byId = when {
            !author.isNullOrEmpty() && !caption.isNullOrEmpty() -> "$author|$caption"
            !author.isNullOrEmpty() -> author
            !caption.isNullOrEmpty() -> caption
            else -> null
        }
        return byId ?: NodeUtils.extractOverlayFingerprint(rootNode)
    }
}
