package io.github.swiftstagrime.termuxrunner.ui.features.customtheme

import androidx.compose.ui.graphics.Color
import io.github.swiftstagrime.termuxrunner.domain.model.CustomTheme

data class CustomThemeUiState(
    val savedThemes: List<CustomTheme> = emptyList(),
    val selectedThemeId: Int? = null,
    val editingTheme: CustomTheme? = null,
    val isNewTheme: Boolean = false
)

data class CustomThemeActions(
    val onBack: () -> Unit,
    val onNewTheme: () -> Unit,
    val onThemeSelect: (CustomTheme) -> Unit,
    val onNameChange: (String) -> Unit,
    val onColorChange: (String, Color) -> Unit,
    val onSave: () -> Unit,
    val onDelete: () -> Unit,
    val onToggleDarkMode: (Boolean) -> Unit,
)