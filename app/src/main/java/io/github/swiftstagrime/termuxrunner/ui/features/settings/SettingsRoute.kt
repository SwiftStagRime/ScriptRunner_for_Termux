package io.github.swiftstagrime.termuxrunner.ui.features.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.swiftstagrime.termuxrunner.R

private const val GITHUB_URL = "https://github.com/SwiftStagRime/ScriptRunner_for_Termux"

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onNavigateToEditor: (Int) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val selectedAccent by viewModel.selectedAccent.collectAsStateWithLifecycle()
    val selectedMode by viewModel.selectedMode.collectAsStateWithLifecycle()
    val ioMessage by viewModel.ioState.collectAsStateWithLifecycle()
    val exportFilename = stringResource(R.string.export_filename)

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { viewModel.exportData(it) } }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.importData(it) } }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.importSingleScript(it) } }

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is SettingsNavEvent.NavigateToEditor -> onNavigateToEditor(event.scriptId)
            }
        }
    }

    LaunchedEffect(ioMessage) {
        ioMessage?.let {
            Toast.makeText(context, it.asString(context), Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    val actions = SettingsActions(
        onAccentChange = viewModel::setAccent,
        onModeChange = viewModel::setMode,
        onTriggerExport = { exportLauncher.launch(exportFilename) },
        onTriggerImport = { importBackupLauncher.launch(arrayOf("application/json")) },
        onTriggerScriptImport = { importFileLauncher.launch(arrayOf("*/*")) },
        onDeveloperClick = { uriHandler.openUri(GITHUB_URL) },
        onBack = onBack
    )

    SettingsScreen(
        selectedAccent = selectedAccent,
        selectedMode = selectedMode,
        actions = actions
    )
}
