package com.example.scrollblock.overlay

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollblock.detector.NodeUtils

/**
 * Kısa Video Girişlerini Gizle (Arayüz Koruma Sistemi) — Obfuscation Korumalı Konumlandırıcı.
 *
 * - Değişken View ID'lerine bağımlı kalmadan çok dilli content-description, alt metin,
 *   alt gezinme çubuğu (BottomBar) hiyerarşik dizilimi ve className kontrolleri yapar.
 * - Konumları sabit koordinatlarla değil, doğrudan `AccessibilityNodeInfo.getBoundsInScreen(Rect)`
 *   üzerinden piksel bazlı dinamik hesaplar.
 * - DM, Hikaye, Klavye (EditText) veya tam ekran video oynatıcı açıkken yanlış yere müdahale
 *   etmemek için zarifçe pasife geçer (Graceful Degradation).
 */
object ShortEntryLocator {

    data class MaskedTarget(
        val bounds: Rect,
        val backgroundColor: Int,
        val label: String
    )

    private val IG_REELS_LABELS = listOf(
        "reels",
        "klip",
        "klipler",
        "reels videoları",
        "reels sekmesi",
        "clips"
    )

    private val IG_SAFE_TAB_LABELS = listOf(
        "ana sayfa", "home", "akış", "feed",
        "ara", "search", "keşfet ve ara", "explore",
        "oluştur", "create", "yeni gönderi", "new post", "kamera",
        "profil", "profile",
        "mesajlar", "messages", "direct"
    )

    private val IG_REELS_EXACT_KEYWORDS = listOf(
        "reels", "klip", "klipler", "clips"
    )

    private val YT_SHORTS_LABELS = listOf(
        "shorts",
        "shorts sekmesi",
        "shorts tab",
        "kısa videolar"
    )

    private val YT_SAFE_TAB_LABELS = listOf(
        "ana sayfa", "home",
        "abonelikler", "subscriptions",
        "oluştur", "create", "yükle", "upload",
        "siz", "you", "kitaplık", "library", "hesap", "account"
    )

    private val TIKTOK_SHORT_TAB_LABELS = listOf(
        "ana sayfa", "home",
        "arkadaşlar", "friends",
        "keşfet", "discover", "explore"
    )

    private val TIKTOK_SAFE_TAB_LABELS = listOf(
        "gelen kutusu", "inbox", "mesajlar", "messages", "bildirimler", "notifications",
        "profil", "profile", "ben", "me"
    )

    private fun isInstagramPackage(pkg: String): Boolean {
        return pkg.lowercase().contains("instagram")
    }

    private fun isYouTubePackage(pkg: String): Boolean {
        val p = pkg.lowercase()
        if (p.contains("music") ||
            p.contains("creator") ||
            p.contains("studio") ||
            p.contains("kids") ||
            p.contains("tv") ||
            p.contains("unplugged")
        ) {
            return false
        }
        return p == "com.google.android.youtube" ||
            p == "app.revanced.android.youtube" ||
            p == "com.vanced.android.youtube"
    }

    private fun isTikTokPackage(pkg: String): Boolean {
        val p = pkg.lowercase()
        return p.contains("musically") ||
            p.contains("trill") ||
            p.contains("tiktok") ||
            p.contains("aweme")
    }

    /**
     * Aktif uygulama penceresinde maskelenecek kısa video giriş sekmelerini bulur.
     * Öğe bulunamazsa veya güvenli bir alt ekrandaysa (DM, klavye vb.) boş liste döner.
     */
    fun findTargetsToMask(
        context: Context,
        rootNode: AccessibilityNodeInfo?,
        packageName: String,
        screenWidth: Int,
        screenHeight: Int,
        blockInstagram: Boolean,
        blockYoutube: Boolean,
        blockTiktok: Boolean
    ): List<MaskedTarget> {
        if (rootNode == null || screenWidth <= 0 || screenHeight <= 0) return emptyList()

        return try {
            // Klavye veya metin girişi odaklanmışsa (örn. DM mesaj yazma, arama çubuğu)
            // alt bar yukarı kayabileceği veya gizlenebileceği için maskeyi anında kaldır.
            if (isTextInputFocused(rootNode)) {
                return emptyList()
            }

            when {
                blockInstagram && isInstagramPackage(packageName) ->
                    locateInstagramReelsEntry(context, rootNode, screenWidth, screenHeight)

                blockYoutube && isYouTubePackage(packageName) ->
                    locateYouTubeShortsEntry(context, rootNode, screenWidth, screenHeight)

                blockTiktok && isTikTokPackage(packageName) ->
                    locateTikTokFeedEntries(context, rootNode, screenWidth, screenHeight)

                else -> emptyList()
            }
        } catch (_: Exception) {
            // Zarif Bozulma (Graceful Degradation): Hata durumunda sessizce pasife geç
            emptyList()
        }
    }

    private fun locateInstagramReelsEntry(
        context: Context,
        rootNode: AccessibilityNodeInfo,
        screenWidth: Int,
        screenHeight: Int
    ): List<MaskedTarget> {
        // DM sohbeti, Hikaye izleyici, Paylaşım penceresi veya Kamera ekranındaysa müdahale etme
        if (NodeUtils.hasVisibleViewId(
                rootNode,
                "row_thread_composer",
                "direct_thread",
                "reel_viewer_media_container",
                "story_viewer",
                "direct_share_sheet",
                "quick_capture_root_container"
            )
        ) {
            return emptyList()
        }

        val bgColor = resolveInstagramThemeColor(context)
        val candidates = collectBottomBarTabs(rootNode, screenWidth, screenHeight)

        // Katman 1: Çok dilli semantik etiket veya bilinen ID ipucu (clips_tab)
        for (candidate in candidates) {
            val labelText = extractNodeLabel(candidate.node).lowercase()
            val idText = (candidate.node.viewIdResourceName ?: "").lowercase()

            val isSafeTab = IG_SAFE_TAB_LABELS.any { labelText == it || labelText.startsWith("$it ") } ||
                idText.contains("feed_tab") ||
                idText.contains("search_tab") ||
                idText.contains("creation_tab") ||
                idText.contains("profile_tab") ||
                idText.contains("direct_tab")

            if (isSafeTab) continue

            val isReelsByLabel = IG_REELS_EXACT_KEYWORDS.any {
                labelText == it || labelText.startsWith("$it ") || labelText.startsWith("$it,") || labelText.endsWith(" $it")
            }
            val isReelsById = idText.endsWith("clips_tab") || idText.endsWith(":id/clips_tab")

            if (isReelsByLabel || isReelsById) {
                return listOf(MaskedTarget(candidate.bounds, bgColor, "Instagram Reels Girişi"))
            }
        }

        // Katman 2: Obfuscation korumalı hiyerarşik alt bar dizilimi
        // Sadece diğer bilinen sekmeler (Home, Search/Explore, Create, Profile) net doğrulandığında
        // ve 4. sekmenin etiketi bilinmeyen bir Reel ikonuysa maskele.
        val sortedRow = extractCleanHorizontalRow(candidates, screenWidth)
        if (sortedRow.size == 5) {
            val firstLabel = extractNodeLabel(sortedRow[0].node).lowercase()
            val isFirstHome = firstLabel.contains("ana sayfa") || firstLabel.contains("home") || firstLabel.contains("feed")

            val fourthTab = sortedRow[3]
            val fourthLabel = extractNodeLabel(fourthTab.node).lowercase()
            val fourthId = (fourthTab.node.viewIdResourceName ?: "").lowercase()
            val isKnownSafe = IG_SAFE_TAB_LABELS.any { fourthLabel.contains(it) } || fourthId.contains("feed_tab") || fourthId.contains("profile_tab")

            if (isFirstHome && !isKnownSafe) {
                return listOf(MaskedTarget(fourthTab.bounds, bgColor, "Instagram Reels Sekmesi"))
            }
        }

        return emptyList()
    }

    private fun locateYouTubeShortsEntry(
        context: Context,
        rootNode: AccessibilityNodeInfo,
        screenWidth: Int,
        screenHeight: Int
    ): List<MaskedTarget> {
        // Tam ekran yatay video oynatıcısında alt bar yoktur
        if (screenWidth > screenHeight) return emptyList()

        val bgColor = resolveYouTubeThemeColor(context)
        val candidates = collectBottomBarTabs(rootNode, screenWidth, screenHeight)

        // Katman 1: Çok dilli "Shorts" sekme etiketi kontrolü
        for (candidate in candidates) {
            val labelText = extractNodeLabel(candidate.node).lowercase()
            val isSafeTab = YT_SAFE_TAB_LABELS.any {
                labelText == it || labelText.startsWith("$it ") || labelText.startsWith("$it,")
            }
            if (isSafeTab) continue

            val isShortsByLabel = YT_SHORTS_LABELS.any {
                labelText == it ||
                    labelText.startsWith("$it ") ||
                    labelText.startsWith("$it,") ||
                    labelText.contains("shorts")
            }
            if (isShortsByLabel) {
                return listOf(MaskedTarget(candidate.bounds, bgColor, "YouTube Shorts Girişi"))
            }
        }

        // Katman 2: Hiyerarşik alt bar dizilimi (Ana Sayfa [0], Shorts [1], + [2], Abonelikler [3], Siz [4])
        val sortedRow = extractCleanHorizontalRow(candidates, screenWidth)
        if (sortedRow.size in 4..5) {
            val firstLabel = extractNodeLabel(sortedRow[0].node).lowercase()
            val secondTab = sortedRow[1]
            val secondLabel = extractNodeLabel(secondTab.node).lowercase()
            val firstIsHome = firstLabel.contains("ana sayfa") || firstLabel.contains("home")
            val secondIsSafe = YT_SAFE_TAB_LABELS.any { secondLabel.contains(it) }

            if (firstIsHome && !secondIsSafe) {
                return listOf(MaskedTarget(secondTab.bounds, bgColor, "YouTube Shorts Sekmesi"))
            }
        }

        return emptyList()
    }

    private fun locateTikTokFeedEntries(
        context: Context,
        rootNode: AccessibilityNodeInfo,
        screenWidth: Int,
        screenHeight: Int
    ): List<MaskedTarget> {
        val bgColor = resolveTikTokThemeColor(context)
        val candidates = collectBottomBarTabs(rootNode, screenWidth, screenHeight)
        val results = mutableListOf<MaskedTarget>()

        for (candidate in candidates) {
            val labelText = extractNodeLabel(candidate.node).lowercase()
            val isSafeTab = TIKTOK_SAFE_TAB_LABELS.any {
                labelText == it || labelText.startsWith("$it ") || labelText.contains(it)
            }
            if (isSafeTab) continue

            val isShortFeedTab = TIKTOK_SHORT_TAB_LABELS.any {
                labelText == it || labelText.startsWith("$it ")
            }
            if (isShortFeedTab) {
                results.add(MaskedTarget(candidate.bounds, bgColor, "TikTok Kısa Video Girişi"))
            }
        }

        return results
    }

    private data class TabCandidate(
        val node: AccessibilityNodeInfo,
        val bounds: Rect
    )

    /**
     * Ekranın alt gezinme çubuğu (Bottom Navigation Bar) bölgesindeki sekme düğmelerini toplar.
     */
    private fun collectBottomBarTabs(
        rootNode: AccessibilityNodeInfo,
        screenWidth: Int,
        screenHeight: Int
    ): List<TabCandidate> {
        val minTop = (screenHeight * 0.78f).toInt()
        val maxBottom = screenHeight
        val minWidth = (screenWidth * 0.08f).toInt()
        val maxWidth = (screenWidth * 0.36f).toInt()
        val minHeight = (screenHeight * 0.03f).toInt().coerceAtLeast(28)
        val maxHeight = (screenHeight * 0.14f).toInt()

        val list = mutableListOf<TabCandidate>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)

        var visited = 0
        while (queue.isNotEmpty() && visited < 350) {
            val current = queue.removeFirst()
            visited++

            if (current.isVisibleToUser) {
                val rect = Rect()
                current.getBoundsInScreen(rect)

                val inBottomZone = rect.top >= minTop &&
                    rect.bottom <= maxBottom &&
                    rect.left >= 0 &&
                    rect.right <= screenWidth &&
                    rect.width() in minWidth..maxWidth &&
                    rect.height() in minHeight..maxHeight

                if (inBottomZone && isLikelyTabNode(current)) {
                    list.add(TabCandidate(current, rect))
                }
            }

            for (i in 0 until current.childCount) {
                current.getChild(i)?.let { queue.add(it) }
            }
        }

        return deduplicateOverlappingCandidates(list)
    }

    private fun isLikelyTabNode(node: AccessibilityNodeInfo): Boolean {
        val cls = node.className?.toString().orEmpty()
        val clickableOrSelected = node.isClickable || node.isSelected || node.isCheckable
        val hasSemanticLabel = !node.contentDescription.isNullOrBlank() || !node.text.isNullOrBlank()
        val isTabClass = cls.contains("Tab", ignoreCase = true) ||
            cls.contains("FrameLayout", ignoreCase = true) ||
            cls.contains("LinearLayout", ignoreCase = true) ||
            cls.contains("ViewGroup", ignoreCase = true) ||
            cls.contains("Button", ignoreCase = true) ||
            cls.contains("ImageView", ignoreCase = true)

        return (clickableOrSelected || hasSemanticLabel) && isTabClass
    }

    private fun deduplicateOverlappingCandidates(candidates: List<TabCandidate>): List<TabCandidate> {
        if (candidates.size <= 1) return candidates
        val result = mutableListOf<TabCandidate>()

        for (candidate in candidates) {
            val existingIndex = result.indexOfFirst { existing ->
                val centerXDiff = kotlin.math.abs(existing.bounds.centerX() - candidate.bounds.centerX())
                val centerYDiff = kotlin.math.abs(existing.bounds.centerY() - candidate.bounds.centerY())
                centerXDiff < 36 && centerYDiff < 36
            }
            if (existingIndex == -1) {
                result.add(candidate)
            } else {
                val existing = result[existingIndex]
                val existingLabel = extractNodeLabel(existing.node)
                val newLabel = extractNodeLabel(candidate.node)
                if (existingLabel.isBlank() && newLabel.isNotBlank()) {
                    result[existingIndex] = candidate
                }
            }
        }
        return result
    }

    /**
     * Aynı yatay hizada (Y ekseninde) sıralanmış alt bar sekmelerini soldan sağa döndürür.
     */
    private fun extractCleanHorizontalRow(
        candidates: List<TabCandidate>,
        screenWidth: Int
    ): List<TabCandidate> {
        if (candidates.size < 4) return emptyList()
        val sortedByX = candidates.sortedBy { it.bounds.centerX() }
        val medianY = sortedByX.map { it.bounds.centerY() }.sorted()[sortedByX.size / 2]

        val aligned = sortedByX.filter {
            kotlin.math.abs(it.bounds.centerY() - medianY) <= 42 &&
                it.bounds.width() <= (screenWidth / 3)
        }
        return aligned
    }

    /**
     * Düğümün kendisindeki veya alt çocuklarındaki (maksimum 2 seviye) metin/contentDescription bilgisini okur.
     */
    private fun extractNodeLabel(node: AccessibilityNodeInfo, depth: Int = 0): String {
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        if (depth >= 2) return ""

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val childLabel = extractNodeLabel(child, depth + 1)
            if (childLabel.isNotEmpty()) return childLabel
        }
        return ""
    }

    private fun isTextInputFocused(rootNode: AccessibilityNodeInfo): Boolean {
        val focused = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val cls = focused.className?.toString().orEmpty()
        return focused.isEditable || cls.contains("EditText", ignoreCase = true)
    }

    private fun isSystemNightMode(context: Context): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }

    private fun resolveInstagramThemeColor(context: Context): Int {
        return Color.TRANSPARENT // Görünmez Şeffaf Dokunma Kalkanı (Tema uyumsuzluğu yok, dokunmayı yutar)
    }

    private fun resolveYouTubeThemeColor(context: Context): Int {
        return Color.TRANSPARENT // Görünmez Şeffaf Dokunma Kalkanı (Tema uyumsuzluğu yok, dokunmayı yutar)
    }

    private fun resolveTikTokThemeColor(context: Context): Int {
        return Color.TRANSPARENT // Görünmez Şeffaf Dokunma Kalkanı (Tema uyumsuzluğu yok, dokunmayı yutar)
    }
}
