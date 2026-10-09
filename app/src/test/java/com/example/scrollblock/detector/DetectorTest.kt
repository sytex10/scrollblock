package com.example.scrollblock.detector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectorTest {

    @Test
    fun testDetectionResultFactory() {
        val detected = DetectionResult.detected("YouTube Shorts", "Reel viewer container")
        assertTrue(detected.isShortFeed)
        assertEquals("YouTube Shorts", detected.platform)
        assertEquals("Reel viewer container", detected.reason)

        val notDetected = DetectionResult.notDetected("Instagram Reels")
        assertFalse(notDetected.isShortFeed)
        assertEquals("Instagram Reels", notDetected.platform)
    }

    @Test
    fun testPackageMatching() {
        val ytPackages = setOf("com.google.android.youtube")
        assertTrue(ytPackages.contains("com.google.android.youtube"))
        assertFalse(ytPackages.contains("com.example.unrelated"))

        val igPackages = setOf("com.instagram.android")
        assertTrue(igPackages.contains("com.instagram.android"))

        val ttPackages = setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")
        assertTrue(ttPackages.contains("com.zhiliaoapp.musically"))
        assertTrue(ttPackages.contains("com.ss.android.ugc.trill"))
    }
}
