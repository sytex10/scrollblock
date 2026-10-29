package com.example.scrollblock.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.scrollblock.preferences.BlockPreferences
import com.example.scrollblock.security.PinManager
import com.example.scrollblock.service.ScrollBlockAccessibilityService
import com.example.scrollblock.util.AccessibilityUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = BlockPreferences.getInstance(application)
    private val pinManager = PinManager.getInstance(application)

    private val _isServiceActive = MutableStateFlow(false)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

    // Platform Anahtarları
    val isYouTubeEnabled: StateFlow<Boolean> = preferences.isYouTubeEnabled
    val isInstagramEnabled: StateFlow<Boolean> = preferences.isInstagramEnabled
    val isTikTokEnabled: StateFlow<Boolean> = preferences.isTikTokEnabled
    val isInstagramDmReelsAllowed: StateFlow<Boolean> = preferences.isInstagramDmReelsAllowed
    val isTikTokDmVideosAllowed: StateFlow<Boolean> = preferences.isTikTokDmVideosAllowed

    // İstatistikler
    val totalBlockedCount: StateFlow<Int> = preferences.totalBlockedCount
    val lastBlockedPlatform: StateFlow<String> = preferences.lastBlockedPlatform
    val lastBlockedTimestamp: StateFlow<Long> = preferences.lastBlockedTimestamp

    // Mola / Cooldown Durumu
    val isBreakActive: StateFlow<Boolean> = preferences.isBreakActive
    val breakEndsAt: StateFlow<Long> = preferences.breakEndsAt
    val breakDurationMinutes: StateFlow<Int> = preferences.breakDurationMinutes
    val breakDailyLimit: StateFlow<Int> = preferences.breakDailyLimit
    val breakUsedToday: StateFlow<Int> = preferences.breakUsedToday

    private val _breakRemainingSeconds = MutableStateFlow(0L)
    val breakRemainingSeconds: StateFlow<Long> = _breakRemainingSeconds.asStateFlow()

    // PIN Güvenlik Durumu
    val isPinEnabled: StateFlow<Boolean> = pinManager.isPinEnabled

    // Tanıtım (Onboarding) Durumu
    val hasCompletedOnboarding: StateFlow<Boolean> = preferences.hasCompletedOnboarding

    init {
        refreshServiceStatus()
        startBreakCountdownTicker()
    }

    private fun startBreakCountdownTicker() {
        viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                preferences.refreshBreakState(now)
                val remainingMs = preferences.getBreakRemainingMs(now)
                _breakRemainingSeconds.value = (remainingMs / 1000L).coerceAtLeast(0L)
                delay(1000L)
            }
        }
    }

    fun refreshServiceStatus() {
        val runningInMemory = ScrollBlockAccessibilityService.isServiceRunning.value
        val enabledInSettings = AccessibilityUtils.isAccessibilityServiceEnabled(getApplication())
        _isServiceActive.value = runningInMemory || enabledInSettings
    }

    // --- Platform Ayarları ---

    fun setYouTubeEnabled(enabled: Boolean) {
        preferences.setYouTubeEnabled(enabled)
    }

    fun setInstagramEnabled(enabled: Boolean) {
        preferences.setInstagramEnabled(enabled)
    }

    fun setTikTokEnabled(enabled: Boolean) {
        preferences.setTikTokEnabled(enabled)
    }

    fun setInstagramDmReelsAllowed(allowed: Boolean) {
        preferences.setInstagramDmReelsAllowed(allowed)
    }

    fun setTikTokDmVideosAllowed(allowed: Boolean) {
        preferences.setTikTokDmVideosAllowed(allowed)
    }

    fun resetStatistics() {
        preferences.resetStatistics()
    }

    fun completeOnboarding() {
        preferences.setOnboardingCompleted(true)
    }

    // --- Mola / Cooldown Aksiyonları ---

    fun startBreak(durationMinutes: Int = preferences.getBreakDurationMinutesSync()): Boolean {
        val success = preferences.startBreak(durationMinutes)
        if (success) {
            val remainingMs = preferences.getBreakRemainingMs()
            _breakRemainingSeconds.value = remainingMs / 1000L
        }
        return success
    }

    fun endBreak() {
        preferences.endBreak()
        _breakRemainingSeconds.value = 0L
    }

    fun setBreakDurationMinutes(min: Int) {
        preferences.setBreakDurationMinutes(min)
    }

    fun setBreakDailyLimit(limit: Int) {
        preferences.setBreakDailyLimit(limit)
    }

    fun canStartBreak(): Boolean = preferences.canStartBreak()

    fun getRemainingBreaksToday(): Int? = preferences.getRemainingBreaksToday()

    // --- PIN Yönetimi ---

    fun verifyPin(pin: String): PinManager.VerifyResult {
        return pinManager.verify(pin)
    }

    fun setPin(pin: String): Boolean {
        return pinManager.setPin(pin)
    }

    fun disablePin() {
        pinManager.disablePin()
    }

    fun getPinLockoutRemainingMs(): Long {
        return pinManager.lockoutRemainingMs()
    }
}
