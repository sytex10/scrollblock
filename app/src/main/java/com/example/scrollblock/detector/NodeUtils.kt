package com.example.scrollblock.detector

import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

object NodeUtils {

    private const val MAX_NODES_TO_VISIT = 180

    /**
     * Traverses the node tree up to MAX_NODES_TO_VISIT nodes and executes [predicate].
     * Returns true as soon as [predicate] returns true.
     */
    fun anyNodeMatches(
        root: AccessibilityNodeInfo?,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): Boolean {
        if (root == null) return false

        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visitedCount = 0

        while (queue.isNotEmpty() && visitedCount < MAX_NODES_TO_VISIT) {
            val current = queue.poll() ?: continue
            visitedCount++

            if (predicate(current)) {
                return true
            }

            val childCount = current.childCount
            for (i in 0 until childCount) {
                val child = current.getChild(i)
                if (child != null) {
                    queue.add(child)
                }
            }
        }

        return false
    }

    /**
     * Checks if any node's viewIdResourceName contains any of the provided identifiers.
     */
    fun hasViewIdContaining(root: AccessibilityNodeInfo?, vararg idSubstrings: String): Boolean {
        return anyNodeMatches(root) { node ->
            val viewId = node.viewIdResourceName?.lowercase() ?: return@anyNodeMatches false
            idSubstrings.any { sub -> viewId.contains(sub.lowercase()) }
        }
    }

    /**
     * Checks if any node's text or contentDescription contains any of the provided words/phrases.
     */
    fun hasTextOrDescContaining(root: AccessibilityNodeInfo?, vararg phrases: String): Boolean {
        return anyNodeMatches(root) { node ->
            val text = node.text?.toString()?.lowercase()
            val desc = node.contentDescription?.toString()?.lowercase()
            phrases.any { phrase ->
                val p = phrase.lowercase()
                (text != null && text.contains(p)) || (desc != null && desc.contains(p))
            }
        }
    }

    /**
     * Finds whether a specific view id exists exactly or ends with ":id/$idName"
     */
    fun hasExactViewId(root: AccessibilityNodeInfo?, vararg idNames: String): Boolean {
        return anyNodeMatches(root) { node ->
            val viewId = node.viewIdResourceName ?: return@anyNodeMatches false
            idNames.any { name ->
                viewId.equals(name, ignoreCase = true) ||
                    viewId.endsWith(":id/$name", ignoreCase = true)
            }
        }
    }
}
