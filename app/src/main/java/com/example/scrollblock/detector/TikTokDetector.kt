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

        val isVideoPlaying = hasVideoContainer && hasVideoActions
        val isHomeTabSelected = NodeUtils.isTabSelected(rootNode, "ana sayfa", "home")
        val targetInboxKeywords = listOf("gelen kutusu", "inbox", "notice")

        // 1. EĞER TAM EKRAN VİDEO OYNATILIYORSA (DM / Sohbet içinden tıklanmış olsa dahi engelle):
        if (isVideoPlaying) {
            if (isHomeTabSelected) {
                // Ana akıştaysa -> Gelen Kutusu'na yönlendir
                return DetectionResult.detected(
                    platform = platformName,
                    reason = "TikTok Ana Sayfa video akışı aktif -> Gelen Kutusu'na yönlendiriliyor",
                    preferTabSwitch = true,
                    targetHomeTabKeywords = targetInboxKeywords
                )
            } else {
                // DM / Sohbet içinden veya başka bir yerden açılan video -> Anında kapat, sohbete geri dön
                return DetectionResult.detected(
                    platform = platformName,
                    reason = "TikTok videosu açıldı (DM/arkadaş videosu dahil) -> Kapatılıyor",
                    preferTabSwitch = false
                )
            }
        }

        // Üst sekmeler (Sizin İçin, Takip Edilenler) varsa -> Gelen Kutusu'na yönlendir
        val hasFeedTabs = NodeUtils.hasTextOrDescContaining(
            rootNode,
            "sizin için",       // TR
            "takip edilenler",  // TR
            "for you",          // EN
            "following"
        )
        if (hasFeedTabs) {
            return DetectionResult.detected(
                platform = platformName,
                reason = "TikTok 'Sizin İçin' / 'Takip Edilenler' akışı aktif -> Gelen Kutusu'na yönlendiriliyor",
                preferTabSwitch = true,
                targetHomeTabKeywords = targetInboxKeywords
            )
        }

        // 2. KESİN GÜVENLİ ALANLAR (Video oynatılmıyorken):
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

        // Sohbet / Mesaj yazma ekranındaysa veya Ayarlardaysa engelleme (video açık değilken yazışabilir)
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

        return DetectionResult.notDetected(platformName)
    }
}
