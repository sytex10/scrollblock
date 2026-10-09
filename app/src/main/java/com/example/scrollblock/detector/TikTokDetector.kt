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

        // 1. KESİN GÜVENLİ ALANLAR (Bu ekranlarda ASLA engelleme yapılmaz):
        // Gelen Kutusu, Mesajlar, Bildirimler, Profil, Ayarlar
        val isSafeNonFeedScreen = NodeUtils.hasTextOrDescContaining(
            rootNode,
            // Gelen Kutusu & Mesajlar (TR & EN)
            "gelen kutusu",
            "inbox",
            "sohbetler",
            "sohbet",
            "mesajlar",
            "mesaj gönder",
            "direkt mesaj",
            "etkinlik",
            "bildirimler",
            "yeni takipçiler",
            "sistem bildirimleri",
            "direct messages",
            "messages",
            "notifications",
            // Profil & Ayarlar
            "profili düzenle",
            "edit profile",
            "profil",
            "ayarlar ve gizlilik",
            "settings and privacy",
            "ayarlar",
            "hesap",
            "gizlilik"
        ) || NodeUtils.hasViewIdContaining(
            rootNode,
            "inbox",
            "chat",
            "message",
            "notice",
            "notification",
            "profile",
            "setting_item",
            "detail_setting",
            "chat_input"
        )

        if (isSafeNonFeedScreen) {
            return DetectionResult.notDetected(platformName)
        }

        // 2. KISA VİDEO AKIŞI GÖSTERGELERİ:
        // Yalnızca Ana Sayfa'daki video oynatıcı veya "Sizin İçin" / "Takip Edilenler" sekmeleri
        val hasFeedTabs = NodeUtils.hasTextOrDescContaining(
            rootNode,
            "sizin için",       // Türkçe For You
            "takip edilenler",  // Türkçe Following
            "for you",          // İngilizce
            "following"
        )

        // Dikey video etkileşim düğmeleri (Beğen, Yorum, Paylaş, Ses)
        val hasVideoActions = NodeUtils.hasTextOrDescContaining(
            rootNode,
            "beğen", "yorum yap", "paylaş", "orijinal ses",
            "like", "comment", "share", "original sound"
        )

        // TikTok video akışı kapayıcısı / View ID'leri
        val hasFeedContainer = NodeUtils.hasViewIdContaining(
            rootNode,
            "aweme_player",
            "video_post_page",
            "vertical_viewpager",
            "feed_view",
            "disk_cover"
        )

        // Video akışında olduğumuzu doğrulayan kesin koşul:
        if (hasFeedTabs || (hasVideoActions && hasFeedContainer)) {
            return DetectionResult.detected(platformName, "TikTok video akışı tespit edildi (Sizin İçin / Ana Akış)")
        }

        // Ek güvence: Eğer üstte 'Sizin İçin' sekmesi görülüyorsa hemen yakala
        if (NodeUtils.hasTextOrDescContaining(rootNode, "sizin için", "for you")) {
            return DetectionResult.detected(platformName, "TikTok 'Sizin İçin' akışı aktif")
        }

        return DetectionResult.notDetected(platformName)
    }
}
