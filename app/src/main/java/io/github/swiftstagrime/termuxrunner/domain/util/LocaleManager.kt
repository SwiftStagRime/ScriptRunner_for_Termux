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
            AppLanguage("zh", "简体中文", "🇨🇳"),
        )

    fun setLocale(languageCode: String) {
        val appLocale = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun getCurrentLanguage(): AppLanguage {
        val locale = AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.getDefault()

        return supportedLanguages.find {
            if (it.code.contains("-")) {
                val parts = it.code.split("-")
                locale.language == parts[0] && (parts.getOrElse(1) { null } == null || locale.country == parts[1])
            } else {
                locale.language == it.code
            }
        } ?: supportedLanguages.first()
    }
}
