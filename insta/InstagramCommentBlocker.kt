package com.example.scrollblock.detector.instagram

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollblock.detector.DetectionResult
import com.example.scrollblock.detector.NodeUtils

/**
 * Instagram Yorum Kalkanı (Tamamen İzole Modül - scrollblock/insta):
 *  - Kullanıcı "Yorumları Engelle" özelliğini açtığında, gönderi veya Reels altındaki
 *    Yorumlar penceresi (bottom sheet) açıldığı an yalnızca 1 kez GERİ basarak yorumları kapatır.
 *  - Tek-geri kilidi (awaitingCommentSheetClose) sayesinde uygulamadan ASLA atmaz.
 */
class InstagramCommentBlocker {

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
                return DetectionResult.notDetected("Instagram Yorum Kalkanı")
            }
            awaitingCommentSheetClose = true
            return DetectionResult.detected(
                platform = "Instagram Yorum Kalkanı",
                reason = "Instagram yorumlar penceresi otomatik kapatıldı",
                preferTabSwitch = false
            )
        }

        awaitingCommentSheetClose = false
        return null
    }

    fun isCommentsSheetOpen(rootNode: AccessibilityNodeInfo): Boolean {
        // Hikaye veya DM ekranındaysak yorum penceresi değildir
        if (NodeUtils.hasVisibleViewId(
                rootNode,
                "reel_viewer_media_container",
                "story_viewer",
                "row_thread_composer",
                "direct_thread"
            )
        ) {
            return false
        }

        // 1. Açık yorum listesi / yorum penceresi view ID'leri
        if (NodeUtils.hasVisibleViewId(
                rootNode,
                "layout_comment_thread",
                "comment_composer_parent",
                "row_comment_textview_comment"
            )
        ) {
            return true
        }

        // 2. "Yorumlar" / "Comments" başlığı + yorum yazma alanı veya comment_composer
        val hasHeader = hasCommentsHeaderTitle(rootNode)
        if (hasHeader && NodeUtils.hasVisibleViewId(rootNode, "comment_composer")) {
            return true
        }

        val hasCommentInputHint = NodeUtils.hasVisibleTextInRegion(
            rootNode, 0.72f, 1.0f,
            "yorum ekle", "add a comment", "yorum yaz", "emoji ekle"
        )
        return hasHeader && hasCommentInputHint
    }

    private fun hasCommentsHeaderTitle(rootNode: AccessibilityNodeInfo): Boolean {
        val winRect = Rect()
        rootNode.getBoundsInScreen(winRect)
        val w = winRect.width()
        val h = winRect.height()
        if (w <= 0 || h <= 0) return false

        val rect = Rect()
        return NodeUtils.anyNodeMatches(rootNode) { node ->
            if (!node.isVisibleToUser) return@anyNodeMatches false
            val t = node.text?.toString()?.trim() ?: ""
            val d = node.contentDescription?.toString()?.trim() ?: ""
            if (!t.equals("yorumlar", ignoreCase = true) &&
                !t.equals("comments", ignoreCase = true) &&
                !d.equals("yorumlar", ignoreCase = true) &&
                !d.equals("comments", ignoreCase = true)
            ) {
                return@anyNodeMatches false
            }
            node.getBoundsInScreen(rect)
            rect.width() > 0 && rect.height() > 0 &&
                rect.exactCenterX() in (w * 0.25f)..(w * 0.75f) &&
                rect.exactCenterY() in (h * 0.02f)..(h * 0.62f)
        }
    }

    fun reset() {
        awaitingCommentSheetClose = false
    }
}

