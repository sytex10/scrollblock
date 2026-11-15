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
        // Hikaye, DM, İçerik Üretme (Kamera/Reels Ekle) veya Müzik/Notlar ekranındaysak yorum penceresi değildir
        if (NodeUtils.hasVisibleViewId(
                rootNode,
                "reel_viewer_media_container",
                "story_viewer",
                "row_thread_composer",
                "direct_thread",
                "quick_capture_fragment",
                "quick_capture_container",
                "camera_view",
                "cam_view",
                "capture_controls",
                "camera_shutter_button",
                "media_picker",
                "gallery_grid",
                "clips_creation",
                "clips_editor",
                "creation_main",
                "music_search",
                "music_browse",
                "audio_page",
                "notes_composer",
                "notes_bubble",
                "note_text",
                "audio_search"
            )
        ) {
            return false
        }

        // Müzik seçimi, notlar veya kamera/oluşturma metinleri varsa yorum penceresi değildir
        if (NodeUtils.hasVisibleTextInRegion(
                rootNode, 0f, 1f,
                "müzik ara", "search music", "şarkı ara", "search songs", "not bırak",
                "düşüncelerini paylaş", "notunu paylaş", "taslak", "taslaklar", "deklanşör",
                "reels oluştur", "yeni reels", "yeni gönderi"
            )
        ) {
            return false
        }

        // 1. Açık yorum listesi / yorum penceresi view ID'leri (ASLA genel bottom_sheet_container_view konulmaz!)
        if (NodeUtils.hasVisibleViewId(
                rootNode,
                "layout_comment_thread",
                "comment_composer_parent",
                "row_comment_textview_comment",
                "comments_list",
                "comments_recycler_view",
                "comment_composer_edittext",
                "comment_composer",
                "clips_comments",
                "row_comment"
            )
        ) {
            return true
        }

        // 2. Açıklama metnine (caption) tıklanınca açılan yorum penceresi / başlığı
        if (hasCommentsHeaderTitle(rootNode)) {
            return true
        }

        // 3. Alt kısımdaki yorum ekleme çubuğu / hint metni
        if (NodeUtils.hasVisibleTextInRegion(
                rootNode, 0.70f, 1.0f,
                "yorum ekle", "add a comment", "yorum yap", "ile yorum yap", "yorum yaz", "emoji ekle"
            )
        ) {
            return true
        }
        return false
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

