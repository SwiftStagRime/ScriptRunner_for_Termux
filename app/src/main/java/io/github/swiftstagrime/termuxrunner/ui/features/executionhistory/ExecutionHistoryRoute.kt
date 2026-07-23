package io.github.swiftstagrime.termuxrunner.ui.features.executionhistory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun ExecutionHistoryRoute(
    onBack: () -> Unit,
    scriptId: Int? = null,
) {
    val viewModel: ExecutionHistoryViewModel = hiltViewModel()

    LaunchedEffect(scriptId) {
        viewModel.setScriptIdFilter(scriptId)
    }

    ExecutionHistoryScreen(
        onBack = onBack,
        scriptName = null,
        viewModel = viewModel,
    )
}
