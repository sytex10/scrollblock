package com.example.scrollblock.detector.tiktok

import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollblock.detector.DetectionResult
import com.example.scrollblock.detector.NodeUtils

/**
 * TikTok Yorum Kalkanı (Tamamen İzole Modül - scrollblock/tiktok):
 *  - Kullanıcı "Yorumları Engelle" özelliğini açtığında, profil veya DM videolarında
 *    Yorumlar penceresi (bottom sheet) açıldığı an yalnızca 1 kez GERİ basarak yorumları kapatır.
 *  - Tek-geri kilidi (awaitingCommentSheetClose) sayesinde uygulamadan ASLA atmaz.
 */
class TikTokCommentBlocker {

    private var awaitingCommentSheetClose: Boolean = false

    fun evaluate(
        rootNode: AccessibilityNodeInfo,
        isCommentsBlocked: Boolean
    ): DetectionResult? {
        if (!isCommentsBlocked) {
            awaitingCommentSheetClose = false
            return null
        }

        if (isCommentsSheetOpen(rootNode)) {
            if (awaitingCommentSheetClose) {
                return DetectionResult.notDetected("TikTok Yorum Kalkanı")
            }
            awaitingCommentSheetClose = true
            return DetectionResult.detected(
                platform = "TikTok Yorum Kalkanı",
                reason = "TikTok yorumlar penceresi otomatik kapatıldı",
                preferTabSwitch = false
            )
        }

        awaitingCommentSheetClose = false
        return null
    }

    fun isCommentsSheetOpen(rootNode: AccessibilityNodeInfo): Boolean {
        // Hikaye (Story) ekranındaysak yorum penceresi değildir
        if (NodeUtils.hasVisibleViewId(
                rootNode,
                "story_viewer", "story_surface", "story_progress"
            ) || NodeUtils.hasVisibleTextInRegion(
                rootNode, 0.78f, 1f,
                "hikayeye yanıt", "reply to story"
            )
        ) {
            return false
        }

        // 1. Açık yorum listesi view ID'leri
        if (NodeUtils.hasVisibleViewId(
                rootNode,
                "comment_list",
                "comment_container"
            )
        ) {
            return true
        }

        // 2. Orta-üst bölgede "yorumlar" / "comments" / "<sayı> yorum" başlığı + altta "yorum ekle" çubuğu
        val hasCommentsHeader = NodeUtils.hasVisibleTextInRegion(
            rootNode, 0.14f, 0.56f,
            "yorumlar", "comments", "yorum", "comment"
        )
        val hasCommentInput = NodeUtils.hasVisibleTextInRegion(
            rootNode, 0.76f, 1f,
            "yorum ekle", "bir yorum ekle", "add comment", "add a comment"
        )

        return hasCommentsHeader && hasCommentInput
    }

    fun reset() {
        awaitingCommentSheetClose = false
    }
}

