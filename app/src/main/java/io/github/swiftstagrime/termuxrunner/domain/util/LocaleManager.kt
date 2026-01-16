package io.github.swiftstagrime.termuxrunner.domain.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

data class AppLanguage(
    val code: String,
    val name: String,
    val flagEmoji: String,
)

object LocaleManager {
    val supportedLanguages =
        listOf(
            AppLanguage("en", "English", "🇺🇸"),
            AppLanguage("ru", "Русский", "🇷🇺"),
            AppLanguage("fr", "Français", "🇫🇷"),
            AppLanguage("de", "Deutsch", "🇩🇪"),
            AppLanguage("es", "Español", "🇪🇸"),
            AppLanguage("pt", "Português", "🇧🇷"),
            AppLanguage("zh-CN", "简体中文", "🇨🇳"),
        )

    fun setLocale(languageCode: String) {
        val appLocale = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun getCurrentLanguage(): AppLanguage {
        val currentTag =
            AppCompatDelegate
                .getApplicationLocales()
                .toLanguageTags()
                .split(",")
                .firstOrNull()
                ?: Locale.getDefault().toLanguageTag()

        return supportedLanguages.find { currentTag.startsWith(it.code) }
            ?: supportedLanguages.first()
    }
}
