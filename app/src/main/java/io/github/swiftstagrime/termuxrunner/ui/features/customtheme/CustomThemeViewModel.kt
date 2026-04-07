package io.github.swiftstagrime.termuxrunner.ui.features.customtheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.swiftstagrime.termuxrunner.di.IoDispatcher
import io.github.swiftstagrime.termuxrunner.domain.model.CustomTheme
import io.github.swiftstagrime.termuxrunner.domain.repository.CustomThemeRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
import io.github.swiftstagrime.termuxrunner.ui.theme.AppTheme
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomThemeViewModel @Inject constructor(
    private val repository: CustomThemeRepository,
    private val userPrefs: UserPreferencesRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomThemeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllCustomThemes().collect { themes ->
                _uiState.update { it.copy(savedThemes = themes) }
            }
        }

        viewModelScope.launch {
            userPrefs.selectedCustomThemeId.collect { id ->
                _uiState.update { it.copy(selectedThemeId = id) }

                if (id != null && _uiState.value.editingTheme?.id != id) {
                    val theme = repository.getThemeById(id)
                    if (theme != null) {
                        _uiState.update { it.copy(editingTheme = theme, isNewTheme = false) }
                    }
                }
            }
        }
    }

    fun createNewTheme() {
        val defaultPrimary = 0xFF6750A4
        val draft = createDefaultTheme("New Theme", defaultPrimary, isDark = true)

        _uiState.update {
            it.copy(
                editingTheme = draft,
                selectedThemeId = null,
                isNewTheme = true
            )
        }
    }

    fun selectTheme(theme: CustomTheme) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update {
                it.copy(
                    editingTheme = theme,
                    isNewTheme = false
                )
            }

            userPrefs.setAccent(AppTheme.CUSTOM)
            userPrefs.setCustomThemeId(theme.id)
        }
    }

    fun updateName(newName: String) {
        _uiState.update { state ->
            state.copy(editingTheme = state.editingTheme?.copy(name = newName))
        }
    }

    fun updateColorField(colorKey: String, color: Color) {
        val argb = color.toArgb().toLong()
        _uiState.update { state ->
            val currentDraft = state.editingTheme ?: return@update state

            val updatedDraft = if (colorKey == "primary") {
                generateFullPalette(currentDraft.name, argb, currentDraft.isDark)
            } else {
                patchThemeField(currentDraft, colorKey, argb)
            }

            state.copy(editingTheme = updatedDraft)
        }
    }

    fun toggleDarkMode(isDark: Boolean) {
        _uiState.update { state ->
            val current = state.editingTheme ?: return@update state
            val updated = generateFullPalette(current.name, current.primary, isDark)
            state.copy(editingTheme = updated)
        }
    }

    fun saveTheme() {
        val themeToSave = _uiState.value.editingTheme ?: return
        viewModelScope.launch(ioDispatcher) {
            repository.saveTheme(themeToSave)
            userPrefs.setAccent(AppTheme.CUSTOM)
        }
    }
    fun deleteTheme() {
        val themeToDelete = _uiState.value.editingTheme ?: return
        if (_uiState.value.isNewTheme) {
            _uiState.update { it.copy(editingTheme = null) }
            return
        }

        viewModelScope.launch(ioDispatcher) {
            repository.deleteTheme(themeToDelete)
            _uiState.update { it.copy(editingTheme = null, selectedThemeId = null) }
        }
    }

    private fun createDefaultTheme(name: String, primary: Long, isDark: Boolean): CustomTheme {
        return generateFullPalette(name, primary, isDark)
    }

    private fun generateFullPalette(name: String, seed: Long, isDark: Boolean): CustomTheme {

        val seedColor = Color(seed.toInt())
        val scheme = if (isDark) darkColorScheme(primary = seedColor) else lightColorScheme(primary = seedColor)

        return CustomTheme(
            id = _uiState.value.editingTheme?.id ?: 0,
            name = name,
            isDark = isDark,
            primary = scheme.primary.toArgb().toLong(),
            onPrimary = scheme.onPrimary.toArgb().toLong(),
            primaryContainer = scheme.primaryContainer.toArgb().toLong(),
            onPrimaryContainer = scheme.onPrimaryContainer.toArgb().toLong(),
            secondary = scheme.secondary.toArgb().toLong(),
            onSecondary = scheme.onSecondary.toArgb().toLong(),
            secondaryContainer = scheme.secondaryContainer.toArgb().toLong(),
            onSecondaryContainer = scheme.onSecondaryContainer.toArgb().toLong(),
            tertiary = scheme.tertiary.toArgb().toLong(),
            onTertiary = scheme.onTertiary.toArgb().toLong(),
            tertiaryContainer = scheme.tertiaryContainer.toArgb().toLong(),
            onTertiaryContainer = scheme.onTertiaryContainer.toArgb().toLong(),
            error = scheme.error.toArgb().toLong(),
            onError = scheme.onError.toArgb().toLong(),
            errorContainer = scheme.errorContainer.toArgb().toLong(),
            onErrorContainer = scheme.onErrorContainer.toArgb().toLong(),
            background = scheme.background.toArgb().toLong(),
            onBackground = scheme.onBackground.toArgb().toLong(),
            surface = scheme.surface.toArgb().toLong(),
            onSurface = scheme.onSurface.toArgb().toLong(),
            surfaceVariant = scheme.surfaceVariant.toArgb().toLong(),
            onSurfaceVariant = scheme.onSurfaceVariant.toArgb().toLong(),
            outline = scheme.outline.toArgb().toLong(),
            outlineVariant = scheme.outlineVariant.toArgb().toLong(),
            surfaceContainer = scheme.surfaceContainer.toArgb().toLong()
        )
    }

    private fun patchThemeField(theme: CustomTheme, key: String, value: Long): CustomTheme {
        return when (key) {
            "onPrimary" -> theme.copy(onPrimary = value)
            "primaryContainer" -> theme.copy(primaryContainer = value)
            "onPrimaryContainer" -> theme.copy(onPrimaryContainer = value)
            "secondary" -> theme.copy(secondary = value)
            "onSecondary" -> theme.copy(onSecondary = value)
            "secondaryContainer" -> theme.copy(secondaryContainer = value)
            "onSecondaryContainer" -> theme.copy(onSecondaryContainer = value)
            "tertiary" -> theme.copy(tertiary = value)
            "onTertiary" -> theme.copy(onTertiary = value)
            "tertiaryContainer" -> theme.copy(tertiaryContainer = value)
            "onTertiaryContainer" -> theme.copy(onTertiaryContainer = value)
            "error" -> theme.copy(error = value)
            "onError" -> theme.copy(onError = value)
            "errorContainer" -> theme.copy(errorContainer = value)
            "onErrorContainer" -> theme.copy(onErrorContainer = value)
            "background" -> theme.copy(background = value)
            "onBackground" -> theme.copy(onBackground = value)
            "surface" -> theme.copy(surface = value)
            "onSurface" -> theme.copy(onSurface = value)
            "surfaceVariant" -> theme.copy(surfaceVariant = value)
            "onSurfaceVariant" -> theme.copy(onSurfaceVariant = value)
            "outline" -> theme.copy(outline = value)
            "outlineVariant" -> theme.copy(outlineVariant = value)
            "surfaceContainer" -> theme.copy(surfaceContainer = value)
            else -> theme
        }
    }
}