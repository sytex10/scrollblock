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

        // 1. KESİN GÜVENLİ ALANLAR (ÖNCELİKLE KORUNACAK):
        // Kullanıcı bu ekranlardayken kesinlikle engellenmemeli, tıklanmamalı ve kaydırması engellenmemeli!

        // A) Mesajlar / DM / Sohbet ekranı:
        val isChat = NodeUtils.hasViewIdContaining(
            rootNode,
            "chat_input",
            "msg_box",
            "message_edit_text",
            "et_message",
            "im_title_bar",
            "chat_room",
            "send_btn",
            "emoji_btn",
            "input_layout"
        ) || NodeUtils.hasTextOrDescContaining(
            rootNode,
            "mesaj gönder",
            "mesaj yaz",
            "type a message",
            "send message"
        )
        if (isChat) {
            return DetectionResult.notDetected(platformName)
        }

        // B) Gelen Kutusu (Inbox) / Aktiviteler / Bildirimler:
        // Tab 4 seçiliyse VEYA bildirim/aktivite/sohbet listesi açıksa
        val isInboxTab = NodeUtils.isTabSelected(
            rootNode,
            "gelen kutusu",
            "inbox",
            "bildirimler"
        )
        val isInboxOrActivityList = NodeUtils.hasViewIdContaining(
            rootNode,
            "notice_list",
            "notification_list",
            "inbox_list",
            "im_conversation_list",
            "activity_list",
            "notice_root",
            "im_root_view",
            "notification_page"
        ) || NodeUtils.hasTextOrDescContaining(
            rootNode,
            "gelen kutusu",
            "tüm aktiviteler",
            "aktiviteler",
            "etkinlik",
            "yeni takipçiler",
            "sistem bildirimleri",
            "direkt mesajlar"
        )

        // Eğer kullanıcı Gelen Kutusu veya Aktiviteler ekranındaysa (ve ana akışta değilse):
        if (isInboxTab || isInboxOrActivityList) {
            // Ana akışın 'Sizin İçin' sekmesi görünmüyorsa kesinlikle Gelen Kutusudur
            val hasFeedTabs = NodeUtils.hasTextOrDescContaining(
                rootNode,
                "sizin için",
                "for you",
                "takip edilenler",
                "following"
            )
            if (!hasFeedTabs) {
                return DetectionResult.notDetected(platformName)
            }
        }

        // C) Profil & Ayarlar:
        val isProfileTab = NodeUtils.isTabSelected(
            rootNode,
            "profil",
            "profile"
        )
        val isProfileOrSettings = NodeUtils.hasViewIdContaining(
            rootNode,
            "profile_header",
            "user_avatar",
            "edit_profile_btn",
            "profile_tab",
            "setting_item",
            "detail_setting"
        ) || NodeUtils.hasTextOrDescContaining(
            rootNode,
            "profili düzenle",
            "edit profile",
            "taslaklar",
            "favoriler",
            "ayarlar ve gizlilik",
            "settings and privacy",
            "hesabı yönet"
        )
        if (isProfileTab || isProfileOrSettings) {
            val hasFeedTabs = NodeUtils.hasTextOrDescContaining(
                rootNode,
                "sizin için",
                "for you"
            )
            if (!hasFeedTabs) {
                return DetectionResult.notDetected(platformName)
            }
        }

        // D) Hikayeler (Stories) izleyicisi:
        val isStoryViewer = NodeUtils.hasViewIdContaining(
            rootNode,
            "story_viewer",
            "story_surface",
            "story_progress",
            "story_container"
        )
        if (isStoryViewer) {
            return DetectionResult.notDetected(platformName)
        }

        // E) Arama / Keşfet:
        val isSearch = NodeUtils.hasViewIdContaining(
            rootNode,
            "search_input",
            "search_edit_text",
            "et_search_kw"
        )
        if (isSearch) {
            return DetectionResult.notDetected(platformName)
        }

        // 2. VİDEO AKIŞI TESPİTİ (Yalnızca gerçek dikey video akışı):
        // A) Üstteki 'Sizin İçin' / 'Takip Edilenler' sekmeleri
        val hasFeedTabs = NodeUtils.hasTextOrDescContaining(
            rootNode,
            "sizin için",
            "for you",
            "takip edilenler",
            "following"
        )

        // B) Alt barda 'Ana Sayfa' sekmesi seçiliyse (TikTok'ta Ana Sayfa sekmesi video akışıdır)
        val isHomeTabSelected = NodeUtils.isTabSelected(
            rootNode,
            "ana sayfa",
            "home"
        )

        // C) Tam ekran video oynatıcı konteynerleri
        val hasVideoContainer = NodeUtils.hasViewIdContaining(
            rootNode,
            "aweme_player",
            "vertical_viewpager",
            "aweme_vertical_view_pager",
            "video_post_page"
        )

        if (hasFeedTabs || isHomeTabSelected || hasVideoContainer) {
            return DetectionResult.detected(
                platform = platformName,
                reason = "TikTok dikey video akışı tespit edildi",
                preferTabSwitch = false
            )
        }

        return DetectionResult.notDetected(platformName)
    }
}

