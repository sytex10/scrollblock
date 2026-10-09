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

        // 1. VİDEO AKIŞI BELİRTİLERİ:
        // Üst sekmeler (Sizin İçin, Takip Edilenler, Arkadaşlar vb.)
        val hasFeedTabs = NodeUtils.hasTextOrDescContaining(
            rootNode,
            "sizin için",       // TR
            "takip edilenler",  // TR
            "for you",          // EN
            "following",
            "arkadaşlar",
            "friends"
        )

        // Dikey video etkileşim düğmeleri (Beğen, Yorum, Paylaş, Ses)
        val hasVideoActions = NodeUtils.hasTextOrDescContaining(
            rootNode,
            "beğen", "yorum yap", "paylaş", "orijinal ses",
            "like", "comment", "share", "original sound"
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

        // Alt barda 'Ana Sayfa' seçiliyse
        val isHomeTabSelected = NodeUtils.isTabSelected(
            rootNode,
            "ana sayfa",
            "home"
        )

        // 2. KESİN GÜVENLİ ALANLAR (YALNIZCA VİDEO AÇIK DEĞİLKEN):
        // Kullanıcı alt bardan Gelen Kutusu veya Profil sekmesini SEÇTİYSE ve video oynatılmıyorsa:
        val isSafeTabSelected = NodeUtils.isTabSelected(
            rootNode,
            "gelen kutusu",
            "inbox",
            "profil",
            "profile"
        )
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

        // Eğer kullanıcı Gelen Kutusunda, Profilde veya Mesajlaşma ekranındaysa VE video izlemiyorsa izin ver:
        if ((isSafeTabSelected || isChatOrSettings) && !hasFeedTabs && !hasVideoActions && !isHomeTabSelected) {
            return DetectionResult.notDetected(platformName)
        }

        // 3. VİDEO AKIŞI VEYA AÇILAN VİDEO TESPİTİ (HER DURUMDA GLOBAL_ACTION_BACK İLE KAPAT):
        // A) 'Sizin İçin' / 'Takip Edilenler' üst sekmeleri varsa
        if (hasFeedTabs) {
            return DetectionResult.detected(
                platform = platformName,
                reason = "TikTok 'Sizin İçin' / 'Takip Edilenler' video akışı aktif",
                preferTabSwitch = false
            )
        }

        // B) Alt barda Ana Sayfa seçiliyse
        if (isHomeTabSelected) {
            return DetectionResult.detected(
                platform = platformName,
                reason = "TikTok Ana Sayfa video akışı aktif",
                preferTabSwitch = false
            )
        }

        // C) Video etkileşim butonları veya video taşıyıcısı varsa (DM'den açılan videolar dahil)
        if (hasVideoActions || hasVideoContainer) {
            return DetectionResult.detected(
                platform = platformName,
                reason = "TikTok video oynatıcı aktif (DM arkadaş videosu dahil)",
                preferTabSwitch = false
            )
        }

        return DetectionResult.notDetected(platformName)
    }
}
