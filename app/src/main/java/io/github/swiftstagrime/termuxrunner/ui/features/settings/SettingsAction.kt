package io.github.swiftstagrime.termuxrunner.ui.features.settings

import io.github.swiftstagrime.termuxrunner.ui.theme.AppTheme
import io.github.swiftstagrime.termuxrunner.ui.theme.ThemeMode

data class SettingsActions(
    val onAccentChange: (AppTheme) -> Unit,
    val onModeChange: (ThemeMode) -> Unit,
    val onTriggerExport: () -> Unit,
    val onTriggerImport: () -> Unit,
    val onTriggerScriptImport: () -> Unit,
    val onDeveloperClick: () -> Unit,
    val onBack: () -> Unit,
)
