package com.example.scrollblock.detector

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

object NodeUtils {

    private const val MAX_NODES_TO_VISIT = 1500

    /** Bottom navigation bar region: lower part of the screen. */
    private const val BOTTOM_NAV_MIN_Y_FRACTION = 0.82f

    private val IGNORED_FINGERPRINT_LABELS = setOf(
        "beğen", "like", "yorum", "yorumlar", "comment", "comments",
        "paylaş", "share", "daha fazla", "more", "takip et", "follow",
        "takip ediliyor", "following", "ses", "audio", "reels", "shorts",
        "ana sayfa", "home", "gelen kutusu", "inbox", "profil", "profile",
        "keşfet", "explore", "ara", "search", "oluştur", "create",
        "mesaj gönder", "mesaj...", "send message", "message..."
    )

    /**
     * Traverses the node tree up to MAX_NODES_TO_VISIT nodes and executes [predicate].
     * Returns true as soon as [predicate] returns true.
     */
    fun anyNodeMatches(
        root: AccessibilityNodeInfo?,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): Boolean = findNode(root, predicate) != null

    /**
     * Breadth-first search returning the first node that satisfies [predicate].
     */
    fun findNode(
        root: AccessibilityNodeInfo?,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (root == null) return null

        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visitedCount = 0

        while (queue.isNotEmpty() && visitedCount < MAX_NODES_TO_VISIT) {
            val current = queue.poll() ?: continue
            visitedCount++

            if (predicate(current)) {
                return current
            }

            val childCount = current.childCount
            for (i in 0 until childCount) {
                val child = current.getChild(i)
                if (child != null) {
                    queue.add(child)
                }
            }
        }
        return null
    }

    /** Height of the active window (used to compute screen regions). */
    fun windowHeight(root: AccessibilityNodeInfo?): Int {
        if (root == null) return 0
        val r = Rect()
        root.getBoundsInScreen(r)
        return r.bottom
    }

    private fun matchesKeywords(node: AccessibilityNodeInfo, keywords: Array<out String>): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        return keywords.any { kw ->
            val k = kw.lowercase()
            text.contains(k) || desc.contains(k)
        }
    }

    private fun matchesViewId(node: AccessibilityNodeInfo, ids: Array<out String>): Boolean {
        val viewId = node.viewIdResourceName ?: return false
        return ids.any { name ->
            viewId.equals(name, ignoreCase = true) ||
                viewId.endsWith(":id/$name", ignoreCase = true)
        }
    }

    /**
     * Checks if any node's viewIdResourceName contains any of the provided identifiers.
     * NOTE: includes hidden nodes. Prefer [hasVisibleViewId] for screen detection.
     */
    fun hasViewIdContaining(root: AccessibilityNodeInfo?, vararg idSubstrings: String): Boolean {
        return anyNodeMatches(root) { node ->
            val viewId = node.viewIdResourceName?.lowercase() ?: return@anyNodeMatches false
            idSubstrings.any { sub -> viewId.contains(sub.lowercase()) }
        }
    }

    /**
     * Like [hasViewIdContaining] but only counts nodes actually VISIBLE to the user.
     * Hidden fragments kept in memory (e.g. Reels after switching tab) are ignored.
     */
    fun hasVisibleViewId(root: AccessibilityNodeInfo?, vararg idSubstrings: String): Boolean {
        val h = windowHeight(root)
        val rect = Rect()
        return anyNodeMatches(root) { node ->
            if (!node.isVisibleToUser) return@anyNodeMatches false
            val viewId = node.viewIdResourceName?.lowercase() ?: return@anyNodeMatches false
            if (!idSubstrings.any { sub -> viewId.contains(sub.lowercase()) }) return@anyNodeMatches false
            node.getBoundsInScreen(rect)
            rect.width() > 0 && rect.height() > 0 && (h <= 0 || rect.bottom > 0)
        }
    }

    /**
     * Checks if any node's text or contentDescription contains any of the provided words/phrases.
     */
    fun hasTextOrDescContaining(root: AccessibilityNodeInfo?, vararg phrases: String): Boolean {
        return anyNodeMatches(root) { node -> matchesKeywords(node, phrases) }
    }

    /**
     * Visible text/description match restricted to a vertical screen band
     * [minYFraction, maxYFraction] (0 = top, 1 = bottom).
     */
    fun hasVisibleTextInRegion(
        root: AccessibilityNodeInfo?,
        minYFraction: Float,
        maxYFraction: Float,
        vararg phrases: String
    ): Boolean {
        val h = windowHeight(root)
        if (h <= 0) return false
        val rect = Rect()
        return anyNodeMatches(root) { node ->
            if (!node.isVisibleToUser) return@anyNodeMatches false
            if (!matchesKeywords(node, phrases)) return@anyNodeMatches false
            node.getBoundsInScreen(rect)
            if (rect.width() <= 0 || rect.height() <= 0) return@anyNodeMatches false
            val cy = rect.exactCenterY()
            cy >= h * minYFraction && cy <= h * maxYFraction
        }
    }

    /**
     * Finds whether a specific view id exists exactly or ends with ":id/$idName"
     */
    fun hasExactViewId(root: AccessibilityNodeInfo?, vararg idNames: String): Boolean {
        return anyNodeMatches(root) { node -> matchesViewId(node, idNames) }
    }

    /**
     * Finds a VISIBLE tab inside the bottom navigation bar region, matched by exact
     * view id (preferred) or by text / contentDescription keywords.
     */
    fun findBottomNavTab(
        root: AccessibilityNodeInfo?,
        ids: List<String>,
        keywords: List<String>
    ): AccessibilityNodeInfo? {
        val h = windowHeight(root)
        if (h <= 0) return null
        val rect = Rect()
        val idArr = ids.toTypedArray()
        val kwArr = keywords.toTypedArray()

        fun inBottomBar(node: AccessibilityNodeInfo): Boolean {
            if (!node.isVisibleToUser) return false
            node.getBoundsInScreen(rect)
            if (rect.width() <= 0 || rect.height() <= 0) return false
            return rect.exactCenterY() >= h * BOTTOM_NAV_MIN_Y_FRACTION
        }

        if (idArr.isNotEmpty()) {
            findNode(root) { n -> matchesViewId(n, idArr) && inBottomBar(n) }?.let { return it }
        }
        if (kwArr.isNotEmpty()) {
            findNode(root) { n -> matchesKeywords(n, kwArr) && inBottomBar(n) }?.let { return it }
        }
        return null
    }

    /**
     * Returns true if [node] (or its direct parent / children) is in selected state.
     * Handles "seçili değil" / "not selected" descriptions correctly.
     */
    fun isNodeSelected(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.isSelected) return true

        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val negative = desc.contains("seçili değil") || desc.contains("not selected")
        if (!negative && (desc.contains("seçildi") || desc.contains("seçili") || desc.contains("selected"))) {
            return true
        }

        try {
            if (node.parent?.isSelected == true) return true
        } catch (_: Exception) { }

        for (i in 0 until node.childCount) {
            val c = node.getChild(i) ?: continue
            if (c.isSelected) return true
        }
        return false
    }

    /**
     * Checks if a tab or button matching [tabKeywords] is currently SELECTED or active.
     */
    fun isTabSelected(root: AccessibilityNodeInfo?, vararg tabKeywords: String): Boolean {
        return anyNodeMatches(root) { node ->
            val viewId = node.viewIdResourceName?.lowercase() ?: ""
            val matches = matchesKeywords(node, tabKeywords) ||
                tabKeywords.any { viewId.contains(it.lowercase()) }
            matches && isNodeSelected(node)
        }
    }

    /**
     * Extracts a Litho-compatible content fingerprint from visible overlay texts/descriptions
     * in the video metadata region (middle-to-bottom of the screen).
     * Changes immediately when the user swipes to the next Reel / TikTok video.
     */
    fun extractOverlayFingerprint(root: AccessibilityNodeInfo?): String? {
        if (root == null) return null
        val h = windowHeight(root)
        if (h <= 0) return null

        val rect = Rect()
        val parts = LinkedHashSet<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0

        while (queue.isNotEmpty() && visited < MAX_NODES_TO_VISIT && parts.size < 4) {
            val node = queue.poll() ?: continue
            visited++

            if (node.isVisibleToUser) {
                node.getBoundsInScreen(rect)
                val cy = rect.exactCenterY()
                if (rect.width() > 0 && rect.height() > 0 && cy in (h * 0.45f)..(h * 0.90f)) {
                    val rawText = node.text?.toString()?.trim()
                    val rawDesc = node.contentDescription?.toString()?.trim()
                    val candidate = when {
                        !rawText.isNullOrEmpty() && rawText.length >= 3 -> rawText
                        !rawDesc.isNullOrEmpty() && rawDesc.length >= 4 -> rawDesc
                        else -> null
                    }
                    if (candidate != null) {
                        val lower = candidate.lowercase()
                        val isGeneric = IGNORED_FINGERPRINT_LABELS.any { lower == it || lower.startsWith("$it ") }
                        if (!isGeneric) {
                            parts.add(candidate.take(40))
                        }
                    }
                }
            }

            val childCount = node.childCount
            for (i in 0 until childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }

        return if (parts.isEmpty()) null else parts.joinToString("|")
    }

    /**
     * Performs ACTION_CLICK on [node] or its nearest clickable ancestor.
     */
    fun clickNode(node: AccessibilityNodeInfo?): Boolean {
        var p = node
        var depth = 0
        while (p != null && depth < 6) {
            if (p.isClickable && p.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            p = try { p.parent } catch (_: Exception) { null }
            depth++
        }
        return false
    }

    /** Center point of [node] on screen, or null if it has no bounds. */
    fun centerOf(node: AccessibilityNodeInfo?): Pair<Float, Float>? {
        if (node == null) return null
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (rect.width() <= 0 || rect.height() <= 0) return null
        return Pair(rect.exactCenterX(), rect.exactCenterY())
    }
}
