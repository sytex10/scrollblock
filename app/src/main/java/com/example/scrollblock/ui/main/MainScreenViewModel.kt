package com.example.scrollblock.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.scrollblock.preferences.BlockPreferences
import com.example.scrollblock.service.ScrollBlockAccessibilityService
import com.example.scrollblock.util.AccessibilityUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = BlockPreferences.getInstance(application)

    private val _isServiceActive = MutableStateFlow(false)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

    val isYouTubeEnabled: StateFlow<Boolean> = preferences.isYouTubeEnabled
    val isInstagramEnabled: StateFlow<Boolean> = preferences.isInstagramEnabled
    val isTikTokEnabled: StateFlow<Boolean> = preferences.isTikTokEnabled

    val totalBlockedCount: StateFlow<Int> = preferences.totalBlockedCount
    val lastBlockedPlatform: StateFlow<String> = preferences.lastBlockedPlatform
    val lastBlockedTimestamp: StateFlow<Long> = preferences.lastBlockedTimestamp

    init {
        refreshServiceStatus()
    }

    fun refreshServiceStatus() {
        val runningInMemory = ScrollBlockAccessibilityService.isServiceRunning.value
        val enabledInSettings = AccessibilityUtils.isAccessibilityServiceEnabled(getApplication())
        _isServiceActive.value = runningInMemory || enabledInSettings
    }

    fun setYouTubeEnabled(enabled: Boolean) {
        preferences.setYouTubeEnabled(enabled)
    }

    fun setInstagramEnabled(enabled: Boolean) {
        preferences.setInstagramEnabled(enabled)
    }

    fun setTikTokEnabled(enabled: Boolean) {
        preferences.setTikTokEnabled(enabled)
    }

    fun resetStatistics() {
        preferences.resetStatistics()
    }
}
