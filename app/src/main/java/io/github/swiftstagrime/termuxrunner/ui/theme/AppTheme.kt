package io.github.swiftstagrime.termuxrunner.ui.theme

import androidx.compose.ui.graphics.Color
import io.github.swiftstagrime.termuxrunner.R

enum class AppTheme(
    val labelRes: Int,
    val primaryColor: Color,
) {
    DYNAMIC(R.string.theme_dynamic, Color.Transparent),
    GREEN(R.string.theme_green, Color(0xFF006C4C)),
    BLUE(R.string.theme_blue, Color(0xFF0061A4)),
    RED(R.string.theme_red, Color(0xFFBC161F)),
    AMOLED(R.string.theme_amoled, Color(0xFF000000)),
    CYBER(R.string.theme_cyber, Color(0xFF00E5FF)),
}

enum class ThemeMode(
    val labelRes: Int,
) {
    SYSTEM(R.string.mode_system),
    LIGHT(R.string.mode_light),
    DARK(R.string.mode_dark),
}
