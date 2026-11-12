package com.example.scrollblock.detector.youtube

import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollblock.detector.DetectionResult
import com.example.scrollblock.detector.NodeUtils

/**
 * YouTube Yorum Kalkanı (Tamamen İzole Modül - scrollblock/yt):
 *  - Kullanıcı "Yorumları Engelle" özelliğini açtığında, normal uzun videolarda veya Shorts'ta
 *    genişletilmiş Yorumlar paneli açıldığı an yalnızca 1 kez GERİ basarak yorum panelini kapatır
 *    (video oynatmaya kesintisiz devam eder).
 *  - Kapalı olan küçük "Yorumlar" önizleme kartına (teaser) ASLA dokunmaz.
 */
class YouTubeCommentBlocker {

    private var awaitingCommentSheetClose: Boolean = false

    fun evaluate(
        rootNode: AccessibilityNodeInfo,
        isCommentsBlocked: Boolean
    ): DetectionResult? {
        if (!isCommentsBlocked) {
            awaitingCommentSheetClose = false
            return null
        }

        // Shorts açıkken veya Shorts yorum paneli açıkken engelleme çalışmalıdır.
        // Yalnızca ve yalnızca normal bir uzun video izleniyorsa (üstte 16:9 oynatıcı, player_collapse_button)
        // ve ekranda hiçbir Shorts/Reel öğesi YOKSA uzun video yorumlarına dokunma.
        val isShortsActive = NodeUtils.hasViewIdContaining(
            rootNode,
            "reel_recycler",
            "shorts_container",
            "reel_player",
            "reel_modern_action_bar",
            "shorts_player",
            "reel_comment",
            "shorts_pivot_button"
        ) || NodeUtils.hasExactViewId(rootNode, "remix_button")

        val isNormalLongVideo = !isShortsActive && NodeUtils.hasViewIdContaining(
            rootNode,
            "player_collapse_button",
            "fullscreen_button",
            "watch_while_time_bar_view"
        )

        if (isNormalLongVideo) {
            awaitingCommentSheetClose = false
            return null
        }

        if (isExpandedCommentsPanelOpen(rootNode)) {
            if (awaitingCommentSheetClose) {
                return DetectionResult.notDetected("YouTube Yorum Kalkanı")
            }
            awaitingCommentSheetClose = true
            return DetectionResult.detected(
                platform = "YouTube Yorum Kalkanı",
                reason = "YouTube Shorts yorumlar paneli otomatik kapatıldı",
                preferTabSwitch = false
            )
        }

        awaitingCommentSheetClose = false
        return null
    }

    /**
     * Yalnızca AÇILMIŞ (genişletilmiş) YouTube Yorumlar panelini tespit eder.
     * Videonun altındaki kapalı küçük yorum kutucuğunu (teaser) ASLA eşleştirmez.
     */
    fun isExpandedCommentsPanelOpen(rootNode: AccessibilityNodeInfo): Boolean {
        // 1. Açık yorum yazma kutusu ve yorum sayfası view ID'leri (Shorts & normal sheet)
        if (NodeUtils.hasVisibleViewId(
                rootNode,
                "comment_composer",
                "comment_dialog",
                "reel_comment_sheet",
                "comments_bottom_sheet",
                "comment_thread",
                "comments_sheet",
                "comment_item",
                "comment_composer_view"
            )
        ) {
            return true
        }

        // 2. Yorum yazma alanı metinleri ("Yorum ekleyin...", "Add a comment...")
        if (NodeUtils.hasVisibleTextInRegion(
                rootNode, 0.25f, 1.0f,
                "yorum ekleyin", "yorum ekle...", "herkese açık bir yorum ekleyin", "herkesin görebileceği bir yorum ekleyin", "add a comment"
            )
        ) {
            return true
        }

        // 3. Yorumlar panel başlığı + kapat veya filtre butonu
        val hasCommentsHeader = NodeUtils.hasVisibleTextInRegion(
            rootNode, 0.12f, 0.55f,
            "yorumlar", "comments"
        )
        val hasCloseOrSort = NodeUtils.hasVisibleTextInRegion(
            rootNode, 0.12f, 0.65f,
            "öne çıkanlar", "en yeni", "top comments", "newest", "kapat", "close"
        ) || NodeUtils.hasVisibleViewId(rootNode, "close_button", "dismiss_button")

        return hasCommentsHeader && hasCloseOrSort
    }

    fun reset() {
        awaitingCommentSheetClose = false
    }
}

