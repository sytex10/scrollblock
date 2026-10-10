package com.example.scrollblock.detector

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollblock.preferences.BlockPreferences

/**
 * TikTok:
 *  - Ana Sayfa (Sizin İçin) video akışı  -> Gelen Kutusu sekmesine geçiş (uygulamadan ATMAZ)
 *  - Sohbetten açılan tam ekran video     -> GERİ (sohbete döner)
 *  - Gelen Kutusu, Sohbet, Profil, Profil düzenleme, Ayarlar, Hikayeler, Arama -> ASLA dokunulmaz
 *
 * Önemli: Sadece EKRANDA GÖRÜNEN düğümler dikkate alınır. "Takip edilen", "Arkadaşlar" gibi
 * profilde/gelen kutusunda da geçen kelimeler artık tetikleyici DEĞİLDİR.
 */
class TikTokDetector(
    private val preferences: BlockPreferences
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
        // Gelen Kutusu veya Profil sekmesi seçiliyse (kaydırma, düzenleme serbest)
        if ((inboxSelected || profileSelected) && !forYouVisibleAtTop) {
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
        if (forYouVisibleAtTop || (homeSelected && !isChat && !isEditOrSettings && !isSearchOrStory)) {
            return DetectionResult.detected(
                platform = platformName,
                reason = "TikTok Ana Sayfa video akışı -> Gelen Kutusu",
                preferTabSwitch = true,
                targetHomeTabKeywords = inboxKeywords
            )
        }

        if (isChat || isEditOrSettings || isSearchOrStory) {
            return DetectionResult.notDetected(platformName)
        }

        // ---- 4. SOHBETTEN/PROFİLDEN AÇILAN TAM EKRAN VİDEO -> GERİ ----
        // Alt bar görünmüyorsa (tam ekran oynatıcı) ve dikey video oynatıcı görünüyorsa
        val bottomBarVisible = inboxTab != null || homeTab != null || profileTab != null
        val hasVisiblePlayer = NodeUtils.hasVisibleViewId(
            rootNode,
            "aweme_player", "aweme_vertical_view_pager", "video_post_page"
        )
        if (!bottomBarVisible && hasVisiblePlayer) {
            return DetectionResult.detected(
                platform = platformName,
                reason = "TikTok tam ekran video (sohbet/arkadaş) -> Geri",
                preferTabSwitch = false
            )
        }

        return DetectionResult.notDetected(platformName)
    }
}
