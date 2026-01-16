package io.github.swiftstagrime.termuxrunner.ui.features.editor

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.model.Category
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.ui.components.ScriptConfigDialog
import io.github.swiftstagrime.termuxrunner.ui.preview.DevicePreviews
import io.github.swiftstagrime.termuxrunner.ui.theme.ScriptRunnerForTermuxTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    script: Script,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSave: (Script) -> Unit,
    categories: List<Category>,
    onAddNewCategory: (String) -> Unit,
    isBatteryUnrestricted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestBatteryUnrestricted: () -> Unit,
    onHeartbeatToggle: (Boolean) -> Unit,
    onProcessImage: suspend (Uri) -> String?,
) {
    var codeState by remember(script.id) {
        mutableStateOf(
            TextFieldValue(
                text = script.code,
                selection = TextRange(script.code.length),
            ),
        )
    }

    var currentScriptObj by remember(script) { mutableStateOf(script) }
    var showConfigDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            currentScriptObj.name.ifBlank { stringResource(R.string.editor_untitled) },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${currentScriptObj.interpreter} • ${currentScriptObj.fileExtension}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_description),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showConfigDialog = true }) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = stringResource(R.string.cd_save),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
    ) { padding ->

        CodeEditor(
            code = codeState,
            onCodeChange = { codeState = it },
            interpreter = currentScriptObj.interpreter,
            modifier = Modifier.padding(padding),
        )

        if (showConfigDialog) {
            ScriptConfigDialog(
                script = currentScriptObj.copy(code = codeState.text),
                categories = categories,
                onAddNewCategory = onAddNewCategory,
                onDismiss = { showConfigDialog = false },
                onSave = { configuredScript ->
                    currentScriptObj = configuredScript
                    showConfigDialog = false
                    onSave(configuredScript)
                },
                onProcessImage = onProcessImage,
                onHeartbeatToggle = onHeartbeatToggle,
                isBatteryUnrestricted = isBatteryUnrestricted,
                onRequestBatteryUnrestricted = onRequestBatteryUnrestricted,
                onRequestNotificationPermission = onRequestNotificationPermission,
            )
        }
    }
}

@DevicePreviews
@Composable
private fun PreviewEditorNewRaw() {
    ScriptRunnerForTermuxTheme {
        EditorScreen(
            script =
                Script(
                    id = 1,
                    name = "Complex Logic",
                    code =
                        """
                        #!/bin/bash
                        echo "Hello World"
                        for i in {1..10}; do
                           echo "Counting i"
                        done
                        """.trimIndent(),
                ),
            onBack = {},
            onSave = {},
            onProcessImage = { null },
            onHeartbeatToggle = {},
            snackbarHostState = SnackbarHostState(),
            isBatteryUnrestricted = false,
            onRequestBatteryUnrestricted = {},
            categories = emptyList(),
            onAddNewCategory = {},
            onRequestNotificationPermission = {},
        )
    }
}
