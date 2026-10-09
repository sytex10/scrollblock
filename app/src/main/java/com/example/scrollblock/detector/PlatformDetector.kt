package com.example.scrollblock.detector

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

data class DetectionResult(
    val isShortFeed: Boolean,
    val platform: String,
    val reason: String = ""
) {
    companion object {
        fun notDetected(platform: String) = DetectionResult(false, platform)
        fun detected(platform: String, reason: String) = DetectionResult(true, platform, reason)
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
