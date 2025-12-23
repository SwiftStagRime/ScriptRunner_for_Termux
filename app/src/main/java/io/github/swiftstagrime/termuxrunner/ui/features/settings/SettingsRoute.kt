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

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val useDynamicColor by viewModel.useDynamicColors.collectAsStateWithLifecycle()
    val ioMessage by viewModel.ioState.collectAsStateWithLifecycle()
    val githubUrl = "https://github.com/SwiftStagRime/ScriptRunner_for_Termux"
    val exportFilename = stringResource(R.string.export_filename)

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportData(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importData(it) }
    }

    LaunchedEffect(ioMessage) {
        ioMessage?.let {
            Toast.makeText(context, it.asString(context), Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    SettingsScreen(
        useDynamicColor = useDynamicColor,
        onDynamicColorChange = viewModel::setDynamicColor,
        onTriggerExport = { exportLauncher.launch(exportFilename) },
        onTriggerImport = { importLauncher.launch(arrayOf("application/json")) },
        onDeveloperClick = { uriHandler.openUri(githubUrl) },
        onBack = onBack
    )
}