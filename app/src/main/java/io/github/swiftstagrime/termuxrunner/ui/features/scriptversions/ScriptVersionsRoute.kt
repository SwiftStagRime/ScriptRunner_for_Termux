package io.github.swiftstagrime.termuxrunner.ui.features.scriptversions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun ScriptVersionsRoute(
    onBack: (scriptId: Int) -> Unit,
    scriptId: Int,
) {
    val viewModel: ScriptVersionsViewModel = hiltViewModel()

    LaunchedEffect(scriptId) {
        viewModel.setScriptId(scriptId)
    }

    LaunchedEffect(viewModel.navEvents) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is ScriptVersionsNavEvent.NavigateBack -> onBack(event.scriptId)
            }
        }
    }

    ScriptVersionsScreen(
        onBack = { viewModel.navigateBack() },
        viewModel = viewModel,
    )
}
