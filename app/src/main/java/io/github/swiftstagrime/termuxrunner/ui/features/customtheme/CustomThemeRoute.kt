package io.github.swiftstagrime.termuxrunner.ui.features.customtheme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CustomThemeRoute(
    onBack: () -> Unit,
    viewModel: CustomThemeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val actions =
        remember(viewModel, onBack) {
            CustomThemeActions(
                onBack = onBack,
                onNewTheme = viewModel::createNewTheme,
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
