package com.example.scrollblock.detector

import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollblock.preferences.BlockPreferences

/**
 * Instagram Reels Engelleyici:
 *  - Alt bardaki Reels sekmesi seçiliyse -> Ana Sayfa sekmesine geçiş (uygulamadan ASLA ATMAZ).
 *  - DM'den gelen / dış bağlantıdan açılan tam ekran Reels:
 *      * "DM İzni (Tek Video)" AÇIK ise: Yalnızca DM'den açılan ilk video izlenebilir.
 *        Kullanıcı bir sonraki videoya kaydırdığı (scroll) veya video içeriği değiştiği anda
 *        anında GERİ atılır (DM sohbetine döner).
 *      * "DM İzni" KAPALI ise: Reel açıldığı an doğrudan GERİ atılır (DM sohbetine döner).
 *  - Ana Sayfa akışı, Hikayeler, DM sohbeti, Profil ve Arama tamamen serbesttir.
 */
class InstagramReelsDetector(
    private val preferences: BlockPreferences,
    private val singleReelGuard: SingleReelGuard = SingleReelGuard()
) : PlatformDetector {

    override val platformName: String = "Instagram Reels"
    override val packageNames: Set<String> = setOf("com.instagram.android")

    /** Kullanıcının Reels'ı DM sohbetinden açıp açmadığını takip eder. */
    private var inDmContext: Boolean = false

    override fun matchesPackage(packageName: String): Boolean {
        return packageName.lowercase().contains("instagram")
    }

    override fun isEnabled(): Boolean = preferences.isInstagramEnabledSync()

    override fun detectShortFeed(
        rootNode: AccessibilityNodeInfo?,
        event: AccessibilityEvent?
    ): DetectionResult {
        if (rootNode == null) return DetectionResult.notDetected(platformName)

        val now = SystemClock.uptimeMillis()

        // ---- 1. ALT BAR SEKMELERİ ----
        val reelsTab = NodeUtils.findBottomNavTab(rootNode, listOf("clips_tab"), listOf("reels"))
        val feedTab = NodeUtils.findBottomNavTab(rootNode, listOf("feed_tab"), listOf("ana sayfa", "home"))
        val profileTab = NodeUtils.findBottomNavTab(rootNode, listOf("profile_tab"), listOf("profil", "profile"))

        val reelsTabSelected = NodeUtils.isNodeSelected(reelsTab)
        val feedTabSelected = NodeUtils.isNodeSelected(feedTab)
        val profileTabSelected = NodeUtils.isNodeSelected(profileTab)

        // Alt bardaki Reels sekmesi seçiliyse -> Her zaman Ana Sayfa sekmesine yönlendir (GERİ tuşu basılmaz)
        if (reelsTabSelected) {
            inDmContext = false
            singleReelGuard.reset()
            return DetectionResult.detected(
                platform = platformName,
                reason = "Instagram Reels sekmesi -> Ana Sayfa",
                preferTabSwitch = true,
                targetTabIds = listOf("feed_tab"),
                targetHomeTabKeywords = listOf("ana sayfa", "home")
            )
        }

        // ---- 2. DM SOHBET / MESAJLAŞMA EKRANI TESPİTİ ----
        val isDmScreen = NodeUtils.hasVisibleViewId(
            rootNode,
            "row_thread_composer",
            "direct_thread",
            "direct_inbox",
            "thread_list",
            "message_list",
            "row_inbox"
        ) || NodeUtils.hasVisibleTextInRegion(
            rootNode, 0.78f, 1.0f,
            "mesaj...", "message...", "mesaj gönder"
        )

        // ---- 3. TAM EKRAN CLIPS (REELS) İZLEYİCİSİ TESPİTİ ----
        val clipsVisible = NodeUtils.hasVisibleViewId(
            rootNode,
            "clips_viewer_view_pager",
            "clips_viewer_fragment",
            "clips_video_container",
            "layout_clips_viewer",
            "clips_viewer_root",
            "clips_item_overlay",
            "clips_ufi_component",
            "clips_author_info"
        )
        val className = event?.className?.toString() ?: ""
        val clipsActivity = event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            className.contains("ClipsViewer", ignoreCase = true)

        val isReelActive = clipsVisible || clipsActivity

        // ---- 4. REEL AKTİF DEĞİLSE: GÜVENLİ EKRANLAR (Ana Sayfa, Hikaye, DM, Profil) ----
        if (!isReelActive) {
            if (isDmScreen) {
                inDmContext = true
            } else if (feedTabSelected || profileTabSelected) {
                inDmContext = false
            }
            singleReelGuard.onViewerGone()
            return DetectionResult.notDetected(platformName)
        }

        // ---- 5. REEL AKTİF İKEN KARAR MEKANİZMASI ----
        // Eğer kullanıcı DM'den gelmediyse ve alt barda Ana Sayfa sekmesi görünüyorsa
        // (ör. Reels sekmesi veya ana ekran üzerinden açıldıysa) -> Ana Sayfa sekmesine tıkla (uygulamadan atma!)
        if (!inDmContext && feedTab != null) {
            singleReelGuard.reset()
            return DetectionResult.detected(
                platform = platformName,
                reason = "Instagram Reels -> Ana Sayfa sekmesine geçiş",
                preferTabSwitch = true,
                targetTabIds = listOf("feed_tab"),
                targetHomeTabKeywords = listOf("ana sayfa", "home")
            )
        }

        // DM İzni (Tek Video) kapalıysa -> doğrudan GERİ ile sohbete/önceki ekrana dön
        val isDmReelAllowed = preferences.isInstagramDmReelsAllowedSync()
        if (!isDmReelAllowed) {
            singleReelGuard.reset()
            return DetectionResult.detected(
                platform = platformName,
                reason = "Instagram tam ekran Reel (DM izni kapalı) -> Geri",
                preferTabSwitch = false
            )
        }

        // DM İzni (Tek Video) AÇIK:
        // a) Sayfalayıcıda 2. veya sonraki öğeye geçildiyse (index > 0) veya scroll yapıldıysa
        if (event?.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            val movedPastFirstItem = event.fromIndex > 0 || (event.toIndex > 0 && event.itemCount > 1)
            val verdict = if (movedPastFirstItem) {
                SingleReelGuard.Verdict.BLOCK
            } else {
                singleReelGuard.onPagerScrolled(now)
            }
            if (verdict == SingleReelGuard.Verdict.BLOCK) {
                return DetectionResult.detected(
                    platform = platformName,
                    reason = "Instagram DM Reels kaydırma engellendi (tek video sınırı)",
                    preferTabSwitch = false
                )
            }
            return DetectionResult.notDetected(platformName)
        }

        // b) Parmak izi kontrolü (Litho uyumlu ekran metinleri + bilinen ID'ler)
        val fingerprint = extractVideoFingerprint(rootNode)
        val verdict = singleReelGuard.onViewerVisible(now, fingerprint)
        if (verdict == SingleReelGuard.Verdict.BLOCK) {
            return DetectionResult.detected(
                platform = platformName,
                reason = "Instagram DM sonraki videoya geçiş engellendi",
                preferTabSwitch = false
            )
        }

        // İlk video serbest
        return DetectionResult.notDetected(platformName)
    }

    /**
     * Ekranda görünen aktif videonun kullanıcı adı, açıklaması veya Litho overlay metinlerinden
     * parmak izi üretir.
     */
    private fun extractVideoFingerprint(rootNode: AccessibilityNodeInfo?): String? {
        if (rootNode == null) return null

        val author = NodeUtils.findNode(rootNode) { node ->
            node.isVisibleToUser && (
                node.viewIdResourceName?.endsWith(":id/row_feed_photo_profile_name") == true ||
                node.viewIdResourceName?.contains("clips_author") == true ||
                node.viewIdResourceName?.contains("user_name") == true
            ) && !node.text.isNullOrEmpty()
        }?.text?.toString()?.trim()

        val caption = NodeUtils.findNode(rootNode) { node ->
            node.isVisibleToUser && (
                node.viewIdResourceName?.contains("clips_caption") == true ||
                node.viewIdResourceName?.contains("video_caption") == true
            ) && !node.text.isNullOrEmpty()
        }?.text?.toString()?.trim()

        val specific = when {
            !author.isNullOrEmpty() && !caption.isNullOrEmpty() -> "$author:$caption"
            !author.isNullOrEmpty() -> author
            !caption.isNullOrEmpty() -> caption
            else -> null
        }

        return specific ?: NodeUtils.extractOverlayFingerprint(rootNode)
    }
}
