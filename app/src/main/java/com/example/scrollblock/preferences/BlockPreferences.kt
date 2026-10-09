package com.example.scrollblock.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BlockPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isYouTubeEnabled = MutableStateFlow(isYouTubeEnabledSync())
    val isYouTubeEnabled: StateFlow<Boolean> = _isYouTubeEnabled.asStateFlow()

    private val _isInstagramEnabled = MutableStateFlow(isInstagramEnabledSync())
    val isInstagramEnabled: StateFlow<Boolean> = _isInstagramEnabled.asStateFlow()

    private val _isTikTokEnabled = MutableStateFlow(isTikTokEnabledSync())
    val isTikTokEnabled: StateFlow<Boolean> = _isTikTokEnabled.asStateFlow()

    private val _totalBlockedCount = MutableStateFlow(getTotalBlockedCountSync())
    val totalBlockedCount: StateFlow<Int> = _totalBlockedCount.asStateFlow()

    private val _lastBlockedPlatform = MutableStateFlow(getLastBlockedPlatformSync())
    val lastBlockedPlatform: StateFlow<String> = _lastBlockedPlatform.asStateFlow()

    private val _lastBlockedTimestamp = MutableStateFlow(getLastBlockedTimestampSync())
    val lastBlockedTimestamp: StateFlow<Long> = _lastBlockedTimestamp.asStateFlow()

    fun isYouTubeEnabledSync(): Boolean =
        prefs.getBoolean(KEY_YOUTUBE_ENABLED, true)

    fun isInstagramEnabledSync(): Boolean =
        prefs.getBoolean(KEY_INSTAGRAM_ENABLED, true)

    fun isTikTokEnabledSync(): Boolean =
        prefs.getBoolean(KEY_TIKTOK_ENABLED, true)

    fun getTotalBlockedCountSync(): Int =
        prefs.getInt(KEY_TOTAL_BLOCKED_COUNT, 0)

    fun getLastBlockedPlatformSync(): String =
        prefs.getString(KEY_LAST_BLOCKED_PLATFORM, "") ?: ""

    fun getLastBlockedTimestampSync(): Long =
        prefs.getLong(KEY_LAST_BLOCKED_TIMESTAMP, 0L)

    fun setYouTubeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_YOUTUBE_ENABLED, enabled).apply()
        _isYouTubeEnabled.value = enabled
    }

    fun setInstagramEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_INSTAGRAM_ENABLED, enabled).apply()
        _isInstagramEnabled.value = enabled
    }

    fun setTikTokEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TIKTOK_ENABLED, enabled).apply()
        _isTikTokEnabled.value = enabled
    }

    @Synchronized
    fun recordBlock(platformName: String) {
        val newCount = getTotalBlockedCountSync() + 1
        val now = System.currentTimeMillis()
        prefs.edit()
            .putInt(KEY_TOTAL_BLOCKED_COUNT, newCount)
            .putString(KEY_LAST_BLOCKED_PLATFORM, platformName)
            .putLong(KEY_LAST_BLOCKED_TIMESTAMP, now)
            .apply()

        _totalBlockedCount.value = newCount
        _lastBlockedPlatform.value = platformName
        _lastBlockedTimestamp.value = now
    }

    fun resetStatistics() {
        prefs.edit()
            .putInt(KEY_TOTAL_BLOCKED_COUNT, 0)
            .putString(KEY_LAST_BLOCKED_PLATFORM, "")
            .putLong(KEY_LAST_BLOCKED_TIMESTAMP, 0L)
            .apply()

        _totalBlockedCount.value = 0
        _lastBlockedPlatform.value = ""
        _lastBlockedTimestamp.value = 0L
    }

    companion object {
        private const val PREFS_NAME = "scrollblock_preferences"
        private const val KEY_YOUTUBE_ENABLED = "pref_youtube_shorts_enabled"
        private const val KEY_INSTAGRAM_ENABLED = "pref_instagram_reels_enabled"
        private const val KEY_TIKTOK_ENABLED = "pref_tiktok_feed_enabled"
        private const val KEY_TOTAL_BLOCKED_COUNT = "pref_total_blocked_count"
        private const val KEY_LAST_BLOCKED_PLATFORM = "pref_last_blocked_platform"
        private const val KEY_LAST_BLOCKED_TIMESTAMP = "pref_last_blocked_timestamp"

        @Volatile
        private var INSTANCE: BlockPreferences? = null

        fun getInstance(context: Context): BlockPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BlockPreferences(context).also { INSTANCE = it }
            }
        }
    }
}
