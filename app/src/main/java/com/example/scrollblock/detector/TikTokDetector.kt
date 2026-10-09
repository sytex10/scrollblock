package com.example.scrollblock.detector

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollblock.preferences.BlockPreferences

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
        if (rootNode == null) {
            return DetectionResult.notDetected(platformName)
        }

        // 1. KESİN GÜVENLİ ALANLAR (Gelen Kutusu, Mesajlar, Profil):
        // Kullanıcı alt bardan Gelen Kutusu veya Profil sekmesini SEÇTİYSE asla engelleme
        val isSafeTabSelected = NodeUtils.isTabSelected(
            rootNode,
            "gelen kutusu",
            "inbox",
            "profil",
            "profile"
        )
        if (isSafeTabSelected) {
            return DetectionResult.notDetected(platformName)
        }

        // Sohbet / Mesaj yazma ekranındaysa veya Ayarlardaysa engelleme
        val isChatOrSettings = NodeUtils.hasViewIdContaining(
            rootNode,
            "chat_input",
            "msg_box",
            "message_edit_text",
            "et_message",
            "im_title_bar",
            "im_conversation_list",
            "notice_list",
            "setting_item",
            "detail_setting"
        ) || NodeUtils.hasTextOrDescContaining(
            rootNode,
            "profili düzenle",
            "edit profile",
            "ayarlar ve gizlilik",
            "settings and privacy"
        )

        if (isChatOrSettings) {
            return DetectionResult.notDetected(platformName)
        }

        // 2. KISA VİDEO AKIŞI (Sizin İçin / Takip Edilenler / Ana Sayfa Akışı):
        // Üst sekmeler (Sizin İçin, Takip Edilenler, Arkadaşlar vb.)
        val hasFeedTabs = NodeUtils.hasTextOrDescContaining(
            rootNode,
            "sizin için",       // TR
            "takip edilenler",  // TR
            "for you",          // EN
            "following"
        )

        // Alt barda 'Ana Sayfa' seçiliyse
        val isHomeTabSelected = NodeUtils.isTabSelected(
            rootNode,
            "ana sayfa",
            "home"
        )

        // Video oynatıcı taşıyıcıları
        val hasVideoContainer = NodeUtils.hasViewIdContaining(
            rootNode,
            "aweme_player",
            "video_post_page",
            "vertical_viewpager",
            "aweme_vertical_view_pager",
            "feed_view",
            "play_view",
            "disk_cover"
        )

        // Dikey video etkileşim düğmeleri (Beğen, Yorum, Paylaş, Ses)
        val hasVideoActions = NodeUtils.hasTextOrDescContaining(
            rootNode,
            "beğen", "yorum yap", "paylaş", "orijinal ses",
            "like", "comment", "share", "original sound"
        )

        val targetTabKeywords = listOf("gelen kutusu", "inbox", "notice")

        // Video akışı algılandığında:
        // A) 'Sizin İçin' / 'Takip Edilenler' üst sekmeleri varsa -> Gelen Kutusu'na yönlendir
        if (hasFeedTabs) {
            return DetectionResult.detected(
                platform = platformName,
                reason = "TikTok 'Sizin İçin' / 'Takip Edilenler' akışı aktif -> Gelen Kutusu'na yönlendiriliyor",
                preferTabSwitch = true,
                targetHomeTabKeywords = targetTabKeywords
            )
        }

        // B) Alt barda Ana Sayfa seçili ve video oynatıcı veya etkileşim butonları varsa -> Gelen Kutusu'na yönlendir
        if (isHomeTabSelected && (hasVideoContainer || hasVideoActions)) {
            return DetectionResult.detected(
                platform = platformName,
                reason = "TikTok Ana Sayfa video akışı aktif -> Gelen Kutusu'na yönlendiriliyor",
                preferTabSwitch = true,
                targetHomeTabKeywords = targetTabKeywords
            )
        }

        // C) Video oynatıcı taşıyıcısı ve aksiyon butonları ekrandaysa -> Gelen Kutusu'na yönlendir
        if (hasVideoContainer && hasVideoActions) {
            return DetectionResult.detected(
                platform = platformName,
                reason = "TikTok dikey video oynatıcı aktif -> Gelen Kutusu'na yönlendiriliyor",
                preferTabSwitch = true,
                targetHomeTabKeywords = targetTabKeywords
            )
        }

        return DetectionResult.notDetected(platformName)
    }
}
