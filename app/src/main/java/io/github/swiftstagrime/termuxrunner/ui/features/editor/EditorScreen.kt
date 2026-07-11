package io.github.swiftstagrime.termuxrunner.ui.features.editor

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    currentPageIndex: Int = 0,
    pageNames: List<String> = emptyList(),
    onPageSelected: (Int) -> Unit = {},
    onAddPage: () -> Unit = {},
    onDeletePage: (Int) -> Unit = {},
    onRenamePage: (Int, String) -> Unit = { _, _ -> },
    onReorderPage: (Int, Int) -> Unit = { _, _ -> },
    pageToDeleteIndex: Int? = null,
    onConfirmDeletePage: () -> Unit = {},
    onDismissDeletePage: () -> Unit = {},
    defaultWrapEnabled: Boolean = false,
) {
    var showConfigDialog by rememberSaveable { mutableStateOf(false) }

    val outerBackgroundColor = MaterialTheme.colorScheme.surface
    val sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest

    Scaffold(
        containerColor = outerBackgroundColor,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                title = {
                    Column {
                        Text(
                            scriptDraft.name.ifBlank { stringResource(R.string.editor_untitled) },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${scriptDraft.interpreter} • ${scriptDraft.fileExtension}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
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
        Surface(
            modifier =
                Modifier
                    .padding(padding)
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 8.dp)
                    .fillMaxSize(),
            color = sheetContainerColor,
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                CodePageTabs(
                    pageCount = scriptDraft.codePages.size,
                    pageNames = pageNames,
                    currentPageIndex = currentPageIndex,
                    onPageSelected = onPageSelected,
                    onAddPage = onAddPage,
                    onDeletePage = onDeletePage,
                    onRenamePage = onRenamePage,
                    onReorderPage = onReorderPage,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
                CodeEditor(
                    code = codeState,
                    onCodeChange = onCodeChange,
                    interpreter = scriptDraft.interpreter,
                    defaultWrapEnabled = defaultWrapEnabled,
                    modifier =
                        Modifier
                            .fillMaxSize(),
                )
            }
        }

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

        pageToDeleteIndex?.let { index ->
            AlertDialog(
                onDismissRequest = onDismissDeletePage,
                title = { Text(stringResource(R.string.cd_delete_page)) },
                text = { Text(stringResource(R.string.cd_confirm_delete_page)) },
                confirmButton = {
                    TextButton(onClick = onConfirmDeletePage) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissDeletePage) {
                        Text(stringResource(R.string.cancel))
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(28.dp),
            )
        }
    }
}

@DevicePreviews
@Composable
fun PreviewEditorNewRaw() {
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
                    codePages = listOf(sampleCode, "# Page 2\necho second"),
                    pageNames = listOf("Main", "Helper"),
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
            currentPageIndex = 0,
            pageNames = listOf("Main", "Helper"),
            onPageSelected = {},
            onAddPage = {},
            onDeletePage = {},
            onRenamePage = { _, _ -> },
        )
    }
}
