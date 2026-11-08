package io.github.swiftstagrime.termuxrunner.ui.features.customtheme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.swiftstagrime.termuxrunner.R

@Composable
fun CustomThemeRoute(
    onBack: () -> Unit,
    viewModel: CustomThemeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val defaultThemeName = stringResource(R.string.theme_default_name)

    val actions =
        remember(viewModel, onBack, defaultThemeName) {
            CustomThemeActions(
                onBack = onBack,
                onNewTheme = { viewModel.createNewTheme(defaultThemeName) },
                onThemeSelect = viewModel::selectTheme,
                onNameChange = viewModel::updateName,
                onColorChange = viewModel::updateColorField,
                onSave = {
                    viewModel.saveTheme()
                },
                onDelete = {
                    viewModel.deleteTheme()
                },
                onToggleDarkMode = viewModel::toggleDarkMode,
            )
        }

    CustomThemeScreen(
        state = state,
        actions = actions,
    )
}
