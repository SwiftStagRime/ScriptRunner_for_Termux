package io.github.swiftstagrime.termuxrunner.ui.features.tiles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.swiftstagrime.termuxrunner.ui.components.ScriptPickerDialog

@Composable
fun TileSettingsRoute(
    onBack: () -> Unit,
    viewModel: TileSettingsViewModel = hiltViewModel()
) {
    val tileMappings by viewModel.tileMappings.collectAsStateWithLifecycle()
    val allScripts by viewModel.allScripts.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    var activeTileSelectionIndex by remember { mutableStateOf<Int?>(null) }

    TileSettingsScreen(
        tileMappings = tileMappings,
        onBack = onBack,
        onClearTile = viewModel::clearTile,
        onTileClicked = { index -> activeTileSelectionIndex = index }
    )

    if (activeTileSelectionIndex != null) {
        ScriptPickerDialog(
            scripts = allScripts,
            categories = allCategories,
            onDismiss = { activeTileSelectionIndex = null },
            onScriptSelected = { script ->
                viewModel.assignScript(activeTileSelectionIndex!!, script.id)
                activeTileSelectionIndex = null
            }
        )
    }
}
