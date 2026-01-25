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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.model.Category
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.ui.features.scriptconfigdialog.ScriptConfigDialog
import io.github.swiftstagrime.termuxrunner.ui.features.scriptconfigdialog.ScriptConfigState
import io.github.swiftstagrime.termuxrunner.ui.preview.DevicePreviews
import io.github.swiftstagrime.termuxrunner.ui.theme.ScriptRunnerForTermuxTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    scriptDraft: Script,
    codeState: TextFieldValue,
    onCodeChange: (TextFieldValue) -> Unit,
    onMetadataChange: (Script) -> Unit,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSave: (Script) -> Unit,
    categories: List<Category>,
    configState: ScriptConfigState?,
    onOpenConfig: () -> Unit,
    onDismissConfig: () -> Unit,
    onAddNewCategory: (String) -> Unit,
    isBatteryUnrestricted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestBatteryUnrestricted: () -> Unit,
    onHeartbeatToggle: (Boolean) -> Unit,
    onProcessImage: suspend (Uri) -> String?,
) {
    var showConfigDialog by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            scriptDraft.name.ifBlank { stringResource(R.string.editor_untitled) },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${scriptDraft.interpreter} • ${scriptDraft.fileExtension}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back_description))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        onOpenConfig()
                        showConfigDialog = true
                    }, modifier = Modifier.testTag("editor_save_btn")) {
                        Icon(Icons.Default.Save, stringResource(R.string.cd_save), tint = MaterialTheme.colorScheme.primary)
                    }
                },
            )
        },
    ) { padding ->
        CodeEditor(
            code = codeState,
            onCodeChange = onCodeChange,
            interpreter = scriptDraft.interpreter,
            modifier = Modifier.padding(padding),
        )

        if (showConfigDialog) {
            ScriptConfigDialog(
                state = configState!!,
                categories = categories,
                onDismiss = {
                    showConfigDialog = false
                    onDismissConfig()
                },
                onSave = { configuredScript ->
                    onMetadataChange(configuredScript)
                    showConfigDialog = false
                    onDismissConfig()
                    onSave(configuredScript)
                },
                onProcessImage = onProcessImage,
                onHeartbeatToggle = onHeartbeatToggle,
                isBatteryUnrestricted = isBatteryUnrestricted,
                onRequestBatteryUnrestricted = onRequestBatteryUnrestricted,
                onRequestNotificationPermission = onRequestNotificationPermission,
                script = scriptDraft,
                onAddNewCategory = onAddNewCategory,
            )
        }
    }
}

@DevicePreviews
@Composable
private fun PreviewEditorNewRaw() {
    val sampleCode =
        """
        #!/bin/bash
        echo "Hello World"
        for i in {1..10}; do
           echo "Counting i"
        done
        """.trimIndent()

    ScriptRunnerForTermuxTheme {
        EditorScreen(
            scriptDraft =
                Script(
                    id = 1,
                    name = "Complex Logic",
                    code = sampleCode,
                ),
            codeState = TextFieldValue(sampleCode),
            onCodeChange = {},
            onMetadataChange = {},
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
            configState = null,
            onOpenConfig = {},
            onDismissConfig = {},
        )
    }
}
