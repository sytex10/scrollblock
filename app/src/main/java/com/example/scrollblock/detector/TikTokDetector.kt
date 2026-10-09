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

        val targetInboxKeywords = listOf("gelen kutusu", "inbox")

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

        // 3. VİDEO AKIŞI VEYA AÇILAN VİDEO TESPİTİ:
        // A) 'Sizin İçin' / 'Takip Edilenler' üst sekmeleri varsa -> Gelen Kutusu'na yönlendir
        if (hasFeedTabs) {
            return DetectionResult.detected(
                platform = platformName,
                reason = "TikTok 'Sizin İçin' / 'Takip Edilenler' video akışı aktif -> Gelen Kutusu'na yönlendiriliyor",
                preferTabSwitch = true,
                targetHomeTabKeywords = targetInboxKeywords
            )
        }

        // B) Alt barda Ana Sayfa seçiliyse -> Gelen Kutusu'na yönlendir
        if (isHomeTabSelected) {
            return DetectionResult.detected(
                platform = platformName,
                reason = "TikTok Ana Sayfa video akışı aktif -> Gelen Kutusu'na yönlendiriliyor",
                preferTabSwitch = true,
                targetHomeTabKeywords = targetInboxKeywords
            )
        }

        // C) Video etkileşim butonları veya video taşıyıcısı varsa
        if (hasVideoActions || hasVideoContainer) {
            if (isHomeTabSelected || hasFeedTabs) {
                // Ana akıştaysa -> Gelen Kutusu'na yönlendir
                return DetectionResult.detected(
                    platform = platformName,
                    reason = "TikTok video akışı aktif -> Gelen Kutusu'na yönlendiriliyor",
                    preferTabSwitch = true,
                    targetHomeTabKeywords = targetInboxKeywords
                )
            } else {
                // DM / Sohbet içinden açılan arkadaş videosu -> Geri çık (Sohbete geri dön)
                return DetectionResult.detected(
                    platform = platformName,
                    reason = "TikTok videosu açıldı (DM arkadaş videosu) -> Kapatılıyor",
                    preferTabSwitch = false
                )
            }
        }

        return DetectionResult.notDetected(platformName)
    }
}
