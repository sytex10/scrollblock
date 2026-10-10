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

        // 1. KESİN GÜVENLİ ALANLAR - ÖNCELİKLİ KONTROL:

        // A) Mesajlar / Sohbet Ekranı:
        val isChat = NodeUtils.hasViewIdContaining(
            rootNode,
            "chat_input",
            "msg_box",
            "message_edit_text",
            "et_message",
            "im_title_bar",
            "chat_room"
        ) || NodeUtils.hasTextOrDescContaining(rootNode, "mesaj gönder", "type a message")
        if (isChat) {
            return DetectionResult.notDetected(platformName)
        }

        // B) Hikayeler (Stories) izleyicisi:
        val isStoryViewer = NodeUtils.hasViewIdContaining(
            rootNode,
            "story_viewer",
            "story_surface",
            "story_progress"
        )
        if (isStoryViewer) {
            return DetectionResult.notDetected(platformName)
        }

        // C) Arama / Keşfet arama kutusu:
        val isSearch = NodeUtils.hasViewIdContaining(
            rootNode,
            "search_input",
            "search_edit_text",
            "et_search_kw"
        )
        if (isSearch) {
            return DetectionResult.notDetected(platformName)
        }

        // 2. VİDEO AKIŞI İMZALARI:
        // Üst sekmeler (Sizin İçin, Takip Edilenler, Arkadaşlar vb.)
        val hasFeedTabs = NodeUtils.hasTextOrDescContaining(
            rootNode,
            "sizin için",
            "takip edilenler",
            "arkadaşlar",
            "for you",
            "following",
            "friends"
        )

        // Alt barda 'Ana Sayfa' seçiliyse
        val isHomeTabSelected = NodeUtils.isTabSelected(
            rootNode,
            "ana sayfa",
            "home"
        )

        // Tam ekran dikey video oynatıcı konteynerleri
        val hasVideoContainer = NodeUtils.hasViewIdContaining(
            rootNode,
            "aweme_player",
            "aweme_vertical_view_pager",
            "vertical_viewpager",
            "feed_view",
            "video_post_page"
        )

        // D) Gelen Kutusu (Inbox) / Aktiviteler kontrolü:
        val isInboxTab = NodeUtils.isTabSelected(rootNode, "gelen kutusu", "inbox")
        val hasInboxList = NodeUtils.hasViewIdContaining(
            rootNode,
            "notice_list",
            "notification_list",
            "inbox_list",
            "im_conversation_list",
            "activity_list"
        )
        // Eğer kullanıcı Gelen Kutusu sekmesinde veya bildirim listesindeyse VE video akışı açık değilse izin ver:
        if ((isInboxTab || hasInboxList) && !hasFeedTabs) {
            return DetectionResult.notDetected(platformName)
        }

        // E) Profil & Ayarlar kontrolü:
        val isProfileTab = NodeUtils.isTabSelected(rootNode, "profil", "profile")
        val hasProfileHeader = NodeUtils.hasViewIdContaining(
            rootNode,
            "profile_header",
            "edit_profile_btn"
        ) || NodeUtils.hasTextOrDescContaining(rootNode, "profili düzenle", "edit profile")
        if ((isProfileTab || hasProfileHeader) && !hasFeedTabs) {
            return DetectionResult.notDetected(platformName)
        }

        // 3. VİDEO AKIŞI TESPİTİ:
        if (hasFeedTabs || isHomeTabSelected || hasVideoContainer) {
            return DetectionResult.detected(
                platform = platformName,
                reason = "TikTok video akışı tespit edildi",
                preferTabSwitch = false
            )
        }

        return DetectionResult.notDetected(platformName)
    }
}

