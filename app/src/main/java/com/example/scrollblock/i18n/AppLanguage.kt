package com.example.scrollblock.i18n

enum class AppLanguage(val code: String, val title: String) {
    TR("tr", "Türkçe"),
    EN("en", "English");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: EN
        }
    }
}
