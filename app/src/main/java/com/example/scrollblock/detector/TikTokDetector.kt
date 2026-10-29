package com.example.scrollblock.detector

import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollblock.preferences.BlockPreferences

/**
 * TikTok Engelleyici:
 *  - Ana Sayfa (Sizin İçin) video akışı -> Gelen Kutusu sekmesine geçiş (uygulamadan ATMAZ).
 *  - DM / Sohbetten açılan tam ekran video:
 *      * "DM İzni (Tek Video)" AÇIK ise: İlk video izlenebilir. Kullanıcı bir sonraki videoya
 *        kaydırdığı (scroll) veya video değiştiği anda anında GERİ atılır (sohbete döner).
 *      * "DM İzni" KAPALI ise: Video açıldığı an doğrudan GERİ atılır.
 *  - Gelen Kutusu, Sohbet mesajlaşma ekranı, Profil, Ayarlar, Hikayeler, Arama -> serbesttir.
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

        val inboxSelected = NodeUtils.isNodeSelected(inboxTab)
        val profileSelected = NodeUtils.isNodeSelected(profileTab)
        val homeSelected = NodeUtils.isNodeSelected(homeTab)

        // Ekranın üst kısmında görünen "Sizin İçin" sekmesi = kesin ana video akışı
        val forYouVisibleAtTop = NodeUtils.hasVisibleTextInRegion(
            rootNode, 0f, 0.22f,
            "sizin için", "for you"
        )

        // ---- 2. KESİN GÜVENLİ SEKME EKRANLARI ----
        // Gelen Kutusu veya Profil sekmesi seçiliyse (ve Sizin İçin akışı aktif değilse)
        if ((inboxSelected || profileSelected) && !forYouVisibleAtTop) {
            singleVideoGuard.onViewerGone()
            return DetectionResult.notDetected(platformName)
        }

        // Profil düzenleme / ayarlar / arama / hikaye ekranları
        val isEditOrSettings = NodeUtils.hasVisibleTextInRegion(
            rootNode, 0f, 1f,
            "profili düzenle", "edit profile", "ayarlar ve gizlilik", "settings and privacy",
            "kullanıcı adı", "username", "biyografi", "bio"
        )
        val isSearchOrStory = NodeUtils.hasVisibleViewId(
            rootNode,
            "search_input", "search_edit_text", "et_search_kw",
            "story_viewer", "story_surface", "story_progress"
        )

        // ---- 3. ANA SAYFA VİDEO AKIŞI -> GELEN KUTUSUNA GEÇ ----
        if (forYouVisibleAtTop || (homeSelected && !isEditOrSettings && !isSearchOrStory)) {
            singleVideoGuard.reset()
            return DetectionResult.detected(
                platform = platformName,
                reason = "TikTok Ana Sayfa video akışı -> Gelen Kutusu",
                preferTabSwitch = true,
                targetHomeTabKeywords = inboxKeywords
            )
        }

        if (isEditOrSettings || isSearchOrStory) {
            singleVideoGuard.onViewerGone()
            return DetectionResult.notDetected(platformName)
        }

        // ---- 4. SOHBETTEN / DM'DEN AÇILAN TAM EKRAN VİDEO ----
        val isFullScreenVideo = hasFullScreenVideoPlayer(rootNode)

        if (isFullScreenVideo) {
            val isDmVideoAllowed = preferences.isTikTokDmVideosAllowedSync()

            // DM İzni (Tek Video) modu KAPALIYSA -> Video açıldığı an direkt kapat (katı koruma)
            if (!isDmVideoAllowed) {
                singleVideoGuard.reset()
                return DetectionResult.detected(
                    platform = platformName,
                    reason = "TikTok tam ekran video (katı koruma: DM izni kapalı) -> Geri",
                    preferTabSwitch = false
                )
            }

            // DM İzni (Tek Video) modu AÇIK:
            // a) Kaydırma olayı (TYPE_VIEW_SCROLLED)
            if (event?.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
                val verdict = singleVideoGuard.onPagerScrolled(now)
                if (verdict == SingleReelGuard.Verdict.BLOCK) {
                    return DetectionResult.detected(
                        platform = platformName,
                        reason = "TikTok DM video kaydırma engellendi (tek video sınırı)",
                        preferTabSwitch = false
                    )
                }
                return DetectionResult.notDetected(platformName)
            }

            // b) Ekrandaki videonun değişimi (yazar/açıklama parmak izi)
            val fingerprint = extractVideoFingerprint(rootNode)
            val verdict = singleVideoGuard.onViewerVisible(now, fingerprint)
            if (verdict == SingleReelGuard.Verdict.BLOCK) {
                return DetectionResult.detected(
                    platform = platformName,
                    reason = "TikTok DM sonraki videoya geçiş engellendi",
                    preferTabSwitch = false
                )
            }

            // İlk video serbest
            return DetectionResult.notDetected(platformName)
        } else {
            // Video oynatıcı ekranda değil (sohbette mesajlar görünüyorsa vb.)
            singleVideoGuard.onViewerGone()
        }

        return DetectionResult.notDetected(platformName)
    }

    /**
     * Tam ekran TikTok video oynatıcısının aktif olup olmadığını tespit eder.
     * Hem bilinen viewId'leri hem de tam ekran dikey ViewPager yapısını kontrol eder.
     */
    private fun hasFullScreenVideoPlayer(rootNode: AccessibilityNodeInfo): Boolean {
        if (NodeUtils.hasVisibleViewId(
                rootNode,
                "aweme_player",
                "aweme_vertical_view_pager",
                "video_post_page",
                "vertical_view_pager",
                "feed_viewpager"
            )
        ) {
            return true
        }

        val rootBounds = Rect()
        rootNode.getBoundsInScreen(rootBounds)
        val screenW = rootBounds.width()
        val screenH = rootBounds.height()
        if (screenW <= 0 || screenH <= 0) return false

        val nodeRect = Rect()
        // Tam ekran dikey ViewPager kontrolü
        return NodeUtils.anyNodeMatches(rootNode) { node ->
            if (!node.isVisibleToUser) return@anyNodeMatches false
            val cls = node.className?.toString() ?: return@anyNodeMatches false
            if (!cls.contains("ViewPager", ignoreCase = true)) return@anyNodeMatches false
            node.getBoundsInScreen(nodeRect)
            nodeRect.width() >= screenW * 0.8f && nodeRect.height() >= screenH * 0.7f
        }
    }

    /**
     * Ekranda oynatılan videonun içerik üreticisi (@kullanici) ve açıklama metninden
     * parmak izi üretir. Kullanıcı kaydırıp video değiştiğinde parmak izi farklılaşır.
     */
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

        return when {
            !author.isNullOrEmpty() && !caption.isNullOrEmpty() -> "$author|$caption"
            !author.isNullOrEmpty() -> author
            !caption.isNullOrEmpty() -> caption
            else -> null
        }
    }
}
