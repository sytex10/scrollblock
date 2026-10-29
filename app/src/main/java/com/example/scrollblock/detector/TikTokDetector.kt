package com.example.scrollblock.detector

import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollblock.preferences.BlockPreferences

/**
 * TikTok Engelleyici:
 *  - Ana Sayfa (Sizin İçin) video akışı -> Gelen Kutusu sekmesine geçiş (uygulamadan ATMAZ).
 *  - Gelen Kutusu, Sohbet, Profil, Profil düzenleme, Ayarlar, Hikayeler, Arama -> %100 SERBESTTİR (kaydırma engellenmez).
 *  - Sohbetten açılan tam ekran video (aweme_player / aweme_vertical_view_pager):
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

        // ---- 2. KESİN GÜVENLİ EKRANLAR ----
        // Gelen Kutusu veya Profil sekmesi seçiliyse (kaydırma, düzenleme tamamen serbest)
        if ((inboxSelected || profileSelected) && !forYouVisibleAtTop) {
            singleVideoGuard.onViewerGone()
            return DetectionResult.notDetected(platformName)
        }

        // Sohbet ekranı (mesaj yazma kutusu görünüyor)
        val isChat = NodeUtils.hasVisibleViewId(
            rootNode,
            "chat_input", "msg_box", "message_edit_text", "et_message", "chat_room"
        ) || NodeUtils.hasVisibleTextInRegion(
            rootNode, 0.6f, 1f,
            "mesaj gönder", "mesaj yaz", "send a message", "type a message", "message..."
        )

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

        // ---- 3. ANA SAYFA VİDEO AKIŞI -> GELEN KUTUSUNA GEÇ ----
        if (forYouVisibleAtTop || (homeSelected && !isChat && !isEditOrSettings && !isSearchOrStory)) {
            singleVideoGuard.reset()
            return DetectionResult.detected(
                platform = platformName,
                reason = "TikTok Ana Sayfa video akışı -> Gelen Kutusu",
                preferTabSwitch = true,
                targetHomeTabKeywords = inboxKeywords
            )
        }

        if (isChat || isEditOrSettings || isSearchOrStory) {
            singleVideoGuard.onViewerGone()
            return DetectionResult.notDetected(platformName)
        }

        // ---- 4. SOHBETTEN AÇILAN TAM EKRAN VİDEO ----
        val bottomBarVisible = inboxTab != null || homeTab != null || profileTab != null
        val hasVisiblePlayer = NodeUtils.hasVisibleViewId(
            rootNode,
            "aweme_player", "aweme_vertical_view_pager", "video_post_page"
        )

        if (!bottomBarVisible && hasVisiblePlayer) {
            val isDmVideoAllowed = preferences.isTikTokDmVideosAllowedSync()

            if (!isDmVideoAllowed) {
                singleVideoGuard.reset()
                return DetectionResult.detected(
                    platform = platformName,
                    reason = "TikTok tam ekran video -> Geri",
                    preferTabSwitch = false
                )
            }

            // DM İzni (Tek Video) modu AÇIK:
            if (event?.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
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
        } else {
            singleVideoGuard.onViewerGone()
        }

        return DetectionResult.notDetected(platformName)
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

        return when {
            !author.isNullOrEmpty() && !caption.isNullOrEmpty() -> "$author|$caption"
            !author.isNullOrEmpty() -> author
            !caption.isNullOrEmpty() -> caption
            else -> null
        }
    }
}
