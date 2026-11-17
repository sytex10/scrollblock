package com.example.scrollblock.i18n

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

object LanguageManager {
    private const val PREFS_NAME = "scrollblock_language_prefs"
    private const val KEY_LANG = "selected_language"

    private val _currentLanguage = MutableStateFlow(AppLanguage.EN)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    val LocalAppStrings = staticCompositionLocalOf<AppStrings> { TurkishStrings }

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_LANG, null)
        val initial = if (saved != null) {
            AppLanguage.fromCode(saved)
        } else {
            // Auto detect system language
            val systemLang = Locale.getDefault().language.lowercase()
            if (systemLang.startsWith("tr")) AppLanguage.TR else AppLanguage.EN
        }
        _currentLanguage.value = initial
    }

    fun getCurrentLanguageSync(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_LANG, null)
        return if (saved != null) {
            AppLanguage.fromCode(saved)
        } else {
            val systemLang = Locale.getDefault().language.lowercase()
            if (systemLang.startsWith("tr")) AppLanguage.TR else AppLanguage.EN
        }
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        _currentLanguage.value = language
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG, language.code).apply()
    }

    fun getStrings(language: AppLanguage): AppStrings {
        return when (language) {
            AppLanguage.TR -> TurkishStrings
            AppLanguage.EN -> EnglishStrings
        }
    }

    fun getStrings(): AppStrings = getStrings(_currentLanguage.value)
}
