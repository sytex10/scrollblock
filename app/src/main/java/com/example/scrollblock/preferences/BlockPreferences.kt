package com.example.scrollblock.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

class BlockPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isYouTubeEnabled = MutableStateFlow(isYouTubeEnabledSync())
    val isYouTubeEnabled: StateFlow<Boolean> = _isYouTubeEnabled.asStateFlow()

    private val _isInstagramEnabled = MutableStateFlow(isInstagramEnabledSync())
    val isInstagramEnabled: StateFlow<Boolean> = _isInstagramEnabled.asStateFlow()

    private val _isTikTokEnabled = MutableStateFlow(isTikTokEnabledSync())
    val isTikTokEnabled: StateFlow<Boolean> = _isTikTokEnabled.asStateFlow()

    private val _isInstagramDmReelsAllowed = MutableStateFlow(isInstagramDmReelsAllowedSync())
    val isInstagramDmReelsAllowed: StateFlow<Boolean> = _isInstagramDmReelsAllowed.asStateFlow()

    private val _isTikTokDmVideosAllowed = MutableStateFlow(isTikTokDmVideosAllowedSync())
    val isTikTokDmVideosAllowed: StateFlow<Boolean> = _isTikTokDmVideosAllowed.asStateFlow()

    private val _totalBlockedCount = MutableStateFlow(getTotalBlockedCountSync())
    val totalBlockedCount: StateFlow<Int> = _totalBlockedCount.asStateFlow()

    private val _lastBlockedPlatform = MutableStateFlow(getLastBlockedPlatformSync())
    val lastBlockedPlatform: StateFlow<String> = _lastBlockedPlatform.asStateFlow()

    private val _lastBlockedTimestamp = MutableStateFlow(getLastBlockedTimestampSync())
    val lastBlockedTimestamp: StateFlow<Long> = _lastBlockedTimestamp.asStateFlow()

    // Mola / Cooldown durumları
    private val _breakEndsAt = MutableStateFlow(getBreakEndsAtSync())
    val breakEndsAt: StateFlow<Long> = _breakEndsAt.asStateFlow()

    private val _isBreakActive = MutableStateFlow(isBreakActiveSync())
    val isBreakActive: StateFlow<Boolean> = _isBreakActive.asStateFlow()

    private val _breakDurationMinutes = MutableStateFlow(getBreakDurationMinutesSync())
    val breakDurationMinutes: StateFlow<Int> = _breakDurationMinutes.asStateFlow()

    private val _breakDailyLimit = MutableStateFlow(getBreakDailyLimitSync())
    val breakDailyLimit: StateFlow<Int> = _breakDailyLimit.asStateFlow()

    private val _breakUsedToday = MutableStateFlow(getBreakUsedTodaySync())
    val breakUsedToday: StateFlow<Int> = _breakUsedToday.asStateFlow()

    private val _hasCompletedOnboarding = MutableStateFlow(hasCompletedOnboardingSync())
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    fun isYouTubeEnabledSync(): Boolean =
        prefs.getBoolean(KEY_YOUTUBE_ENABLED, true)

    fun isInstagramEnabledSync(): Boolean =
        prefs.getBoolean(KEY_INSTAGRAM_ENABLED, true)

    fun isTikTokEnabledSync(): Boolean =
        prefs.getBoolean(KEY_TIKTOK_ENABLED, true)

    fun isInstagramDmReelsAllowedSync(): Boolean =
        prefs.getBoolean(KEY_INSTAGRAM_DM_REELS_ALLOWED, false)

    fun isTikTokDmVideosAllowedSync(): Boolean =
        prefs.getBoolean(KEY_TIKTOK_DM_VIDEOS_ALLOWED, false)

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

    fun setInstagramDmReelsAllowed(allowed: Boolean) {
        prefs.edit().putBoolean(KEY_INSTAGRAM_DM_REELS_ALLOWED, allowed).apply()
        _isInstagramDmReelsAllowed.value = allowed
    }

    fun setTikTokDmVideosAllowed(allowed: Boolean) {
        prefs.edit().putBoolean(KEY_TIKTOK_DM_VIDEOS_ALLOWED, allowed).apply()
        _isTikTokDmVideosAllowed.value = allowed
    }

    // --- Mola / Cooldown Yönetimi ---

    private fun currentDayKey(): Int {
        val cal = Calendar.getInstance()
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return BreakPolicy.dayKey(y, m, d)
    }

    private fun getBreakState(): BreakPolicy.State {
        return BreakPolicy.State(
            endsAt = prefs.getLong(KEY_BREAK_ENDS_AT, 0L),
            startedAt = prefs.getLong(KEY_BREAK_STARTED_AT, 0L),
            dayKey = prefs.getInt(KEY_BREAK_DAY_KEY, 0),
            usedToday = prefs.getInt(KEY_BREAK_USED_TODAY, 0)
        )
    }

    fun getBreakEndsAtSync(): Long = prefs.getLong(KEY_BREAK_ENDS_AT, 0L)

    fun getBreakDurationMinutesSync(): Int =
        prefs.getInt(KEY_BREAK_DURATION_MINUTES, BreakPolicy.DEFAULT_DURATION_MIN)

    fun setBreakDurationMinutes(min: Int) {
        prefs.edit().putInt(KEY_BREAK_DURATION_MINUTES, min).apply()
        _breakDurationMinutes.value = min
    }

    fun getBreakDailyLimitSync(): Int =
        prefs.getInt(KEY_BREAK_DAILY_LIMIT, BreakPolicy.DEFAULT_DAILY_LIMIT)

    fun setBreakDailyLimit(limit: Int) {
        prefs.edit().putInt(KEY_BREAK_DAILY_LIMIT, limit).apply()
        _breakDailyLimit.value = limit
    }

    fun getBreakUsedTodaySync(): Int {
        val state = getBreakState()
        return BreakPolicy.usedToday(state, currentDayKey())
    }

    fun isBreakActiveSync(now: Long = System.currentTimeMillis()): Boolean {
        val state = getBreakState()
        return BreakPolicy.isActive(state, now)
    }

    fun getBreakRemainingMs(now: Long = System.currentTimeMillis()): Long {
        val endsAt = getBreakEndsAtSync()
        return (endsAt - now).coerceAtLeast(0L)
    }

    fun canStartBreak(now: Long = System.currentTimeMillis()): Boolean {
        val state = getBreakState()
        return BreakPolicy.canStart(state, now, currentDayKey(), getBreakDailyLimitSync())
    }

    fun getRemainingBreaksToday(): Int? {
        val state = getBreakState()
        return BreakPolicy.remainingToday(state, currentDayKey(), getBreakDailyLimitSync())
    }

    @Synchronized
    fun startBreak(
        durationMinutes: Int = getBreakDurationMinutesSync(),
        now: Long = System.currentTimeMillis()
    ): Boolean {
        val state = getBreakState()
        val todayKey = currentDayKey()
        val newState = BreakPolicy.start(state, now, todayKey, getBreakDailyLimitSync(), durationMinutes)
            ?: return false

        prefs.edit()
            .putLong(KEY_BREAK_ENDS_AT, newState.endsAt)
            .putLong(KEY_BREAK_STARTED_AT, newState.startedAt)
            .putInt(KEY_BREAK_DAY_KEY, newState.dayKey)
            .putInt(KEY_BREAK_USED_TODAY, newState.usedToday)
            .apply()

        _breakEndsAt.value = newState.endsAt
        _isBreakActive.value = true
        _breakUsedToday.value = newState.usedToday
        return true
    }

    @Synchronized
    fun endBreak() {
        val state = getBreakState()
        val newState = BreakPolicy.end(state)

        prefs.edit()
            .putLong(KEY_BREAK_ENDS_AT, newState.endsAt)
            .putLong(KEY_BREAK_STARTED_AT, newState.startedAt)
            .apply()

        _breakEndsAt.value = 0L
        _isBreakActive.value = false
    }

    fun refreshBreakState(now: Long = System.currentTimeMillis()) {
        val active = isBreakActiveSync(now)
        _isBreakActive.value = active
        _breakEndsAt.value = getBreakEndsAtSync()
        _breakUsedToday.value = getBreakUsedTodaySync()
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

    fun hasCompletedOnboardingSync(): Boolean =
        prefs.getBoolean(KEY_HAS_COMPLETED_ONBOARDING, false)

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_COMPLETED_ONBOARDING, completed).apply()
        _hasCompletedOnboarding.value = completed
    }

    companion object {
        private const val PREFS_NAME = "scrollblock_preferences"
        private const val KEY_HAS_COMPLETED_ONBOARDING = "pref_has_completed_onboarding"
        private const val KEY_YOUTUBE_ENABLED = "pref_youtube_shorts_enabled"
        private const val KEY_INSTAGRAM_ENABLED = "pref_instagram_reels_enabled"
        private const val KEY_TIKTOK_ENABLED = "pref_tiktok_feed_enabled"
        private const val KEY_INSTAGRAM_DM_REELS_ALLOWED = "pref_instagram_dm_reels_allowed"
        private const val KEY_TIKTOK_DM_VIDEOS_ALLOWED = "pref_tiktok_dm_videos_allowed"
        private const val KEY_TOTAL_BLOCKED_COUNT = "pref_total_blocked_count"
        private const val KEY_LAST_BLOCKED_PLATFORM = "pref_last_blocked_platform"
        private const val KEY_LAST_BLOCKED_TIMESTAMP = "pref_last_blocked_timestamp"

        private const val KEY_BREAK_ENDS_AT = "pref_break_ends_at"
        private const val KEY_BREAK_STARTED_AT = "pref_break_started_at"
        private const val KEY_BREAK_DURATION_MINUTES = "pref_break_duration_minutes"
        private const val KEY_BREAK_DAILY_LIMIT = "pref_break_daily_limit"
        private const val KEY_BREAK_DAY_KEY = "pref_break_day_key"
        private const val KEY_BREAK_USED_TODAY = "pref_break_used_today"

        @Volatile
        private var INSTANCE: BlockPreferences? = null

        fun getInstance(context: Context): BlockPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BlockPreferences(context).also { INSTANCE = it }
            }
        }
    }
}
