package com.example.scrollblock.detector

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

data class DetectionResult(
    val isShortFeed: Boolean,
    val platform: String,
    val reason: String = "",
    /** true -> switch to a safe tab instead of pressing BACK (prevents app exit). */
    val preferTabSwitch: Boolean = false,
    /** Text / contentDescription keywords of the safe target tab. */
    val targetHomeTabKeywords: List<String> = emptyList(),
    /** Exact view ids of the safe target tab (preferred over keywords). */
    val targetTabIds: List<String> = emptyList()
) {
    companion object {
        fun notDetected(platform: String) = DetectionResult(false, platform)

        fun detected(
            platform: String,
            reason: String,
            preferTabSwitch: Boolean = false,
            targetHomeTabKeywords: List<String> = emptyList(),
            targetTabIds: List<String> = emptyList()
        ) = DetectionResult(
            isShortFeed = true,
            platform = platform,
            reason = reason,
            preferTabSwitch = preferTabSwitch,
            targetHomeTabKeywords = targetHomeTabKeywords,
            targetTabIds = targetTabIds
        )
    }
}

interface PlatformDetector {
    val platformName: String
    val packageNames: Set<String>

    fun matchesPackage(packageName: String): Boolean {
        return packageNames.contains(packageName)
    }

    fun isEnabled(): Boolean

    /**
     * Inspects the accessibility node tree and event to determine if the active
     * screen is a short-form video feed.
     */
    fun detectShortFeed(rootNode: AccessibilityNodeInfo?, event: AccessibilityEvent?): DetectionResult
}
