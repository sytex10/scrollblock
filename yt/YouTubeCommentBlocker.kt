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

        if (isExpandedCommentsPanelOpen(rootNode)) {
            if (awaitingCommentSheetClose) {
                return DetectionResult.notDetected("YouTube Yorum Kalkanı")
            }
            awaitingCommentSheetClose = true
            return DetectionResult.detected(
                platform = "YouTube Yorum Kalkanı",
                reason = "YouTube yorumlar paneli otomatik kapatıldı",
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
        // 1. Açık yorum yazma kutusu view ID'leri
        if (NodeUtils.hasVisibleViewId(
                rootNode,
                "comment_composer",
                "comment_dialog"
            )
        ) {
            return true
        }

        // 2. Yorum yazma alanı metinleri ("Yorum ekleyin...", "Add a comment...")
        if (NodeUtils.hasVisibleTextInRegion(
                rootNode, 0.25f, 1.0f,
                "yorum ekleyin", "yorum ekle...", "herkese açık bir yorum ekleyin", "add a comment"
            )
        ) {
            return true
        }

        // 3. Yorumlar panel başlığı + sıralama filtre çipleri ("Öne çıkanlar" / "En yeni" / "Top" / "Newest")
        val hasCommentsHeader = NodeUtils.hasVisibleTextInRegion(
            rootNode, 0.18f, 0.55f,
            "yorumlar", "comments"
        )
        val hasSortChips = NodeUtils.hasVisibleTextInRegion(
            rootNode, 0.22f, 0.62f,
            "öne çıkanlar", "en yeni", "top comments", "newest"
        )

        return hasCommentsHeader && hasSortChips
    }

    fun reset() {
        awaitingCommentSheetClose = false
    }
}

