package com.example.scrollblock.android12.core

import android.os.Build
import com.example.scrollblock.android12.insta.Android12InstagramDetector
import com.example.scrollblock.android12.tiktok.Android12TikTokDetector
import com.example.scrollblock.android12.yt.Android12YouTubeDetector
import com.example.scrollblock.detector.PlatformDetector
import com.example.scrollblock.preferences.BlockPreferences

/**
 * Android 12 ve Altı (Build.VERSION.SDK_INT <= 32) İçin İzole Servis Yönlendiricisi.
 */
object Android12ServiceRouter {

    fun isAndroid12OrBelow(): Boolean {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2
    }

    fun createAndroid12Detectors(preferences: BlockPreferences): List<PlatformDetector> {
        return listOf(
            Android12YouTubeDetector(preferences),
            Android12InstagramDetector(preferences),
            Android12TikTokDetector(preferences)
        )
    }
}
