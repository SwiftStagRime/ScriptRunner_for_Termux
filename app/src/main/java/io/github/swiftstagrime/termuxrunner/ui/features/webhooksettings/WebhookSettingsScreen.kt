package io.github.swiftstagrime.termuxrunner.ui.features.webhooksettings

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.ui.theme.ScriptRunnerForTermuxTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebhookSettingsScreen(
    state: WebhookSettingsState,
    actions: WebhookSettingsActions,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.webhook_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_description),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Surface(
            modifier = Modifier.padding(padding),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
            ) {
                ServiceStatusCard(state, actions)

                Spacer(modifier = Modifier.height(16.dp))

                ConfigurationSection(state, actions)

                Spacer(modifier = Modifier.height(16.dp))

                TokenSection(state, actions)

                if (!state.token.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ExamplesSection(state.port, state.token)
                }
            }
        }
    }
}

@Composable
private fun ServiceStatusCard(
    state: WebhookSettingsState,
    actions: WebhookSettingsActions,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (state.isRunning) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    },
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.webhook_enable_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (state.isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (state.isRunning) Icons.Default.CheckCircle else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = if (state.isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text =
                                if (state.isRunning) {
                                    stringResource(R.string.webhook_running_indicator, state.port)
                                } else {
                                    stringResource(R.string.webhook_stopped_indicator)
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Switch(
                    checked = state.isEnabled,
                    onCheckedChange = actions.onToggleEnabled,
                )
            }
        }
    }
}

@Composable
private fun ConfigurationSection(
    state: WebhookSettingsState,
    actions: WebhookSettingsActions,
) {
    Text(
        text = stringResource(R.string.webhook_config_label),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.webhook_port_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = state.port.toString(),
                onValueChange = { it -> actions.onPortChange(it.toIntOrNull() ?: 0) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor =
                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.3f,
                            ),
                    ),
                singleLine = true,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.webhook_bind_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value =
                    if (state.lanAccess) {
                        stringResource(R.string.webhook_bind_all)
                    } else {
                        stringResource(
                            R.string.webhook_bind_local,
                        )
                    },
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    Switch(
                        checked = state.lanAccess,
                        onCheckedChange = actions.onToggleLanAccess,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                },
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor =
                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.3f,
                            ),
                        disabledIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    ),
                singleLine = true,
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = stringResource(R.string.webhook_lan_access_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun TokenSection(
    state: WebhookSettingsState,
    actions: WebhookSettingsActions,
) {
    val clipboard = LocalClipboard.current
    var showCopied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(showCopied) {
        if (showCopied) {
            delay(1500)
            showCopied = false
        }
    }

    Text(
        text = stringResource(R.string.webhook_auth_label),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )

    Spacer(modifier = Modifier.height(12.dp))

    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.webhook_token_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )

                Row {
                    if (!state.token.isNullOrBlank()) {
                        IconButton(onClick = {
                            scope.launch {
                                val clipData = ClipData.newPlainText("webhook_token", state.token)
                                clipboard.setClipEntry(ClipEntry(clipData))
                                showCopied = true
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = stringResource(R.string.webhook_copy_token),
                                tint = if (showCopied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = actions.onRegenerateToken) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.webhook_regenerate_token),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Button(
                            onClick = actions.onRegenerateToken,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(stringResource(R.string.webhook_generate_token))
                        }
                    }
                }
            }

            if (!state.token.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                SelectionContainer {
                    Text(
                        text = state.token,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (showCopied) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.copied_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    if (!state.token.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.webhook_auth_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExamplesSection(
    port: Int,
    token: String?,
) {
    if (token.isNullOrBlank()) return
    val clipboard = LocalClipboard.current
    var showCopied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(showCopied) {
        if (showCopied) {
            delay(1500)
            showCopied = false
        }
    }

    Text(
        text = stringResource(R.string.webhook_examples_label),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )

    Spacer(modifier = Modifier.height(12.dp))

    val curlScript =
        """curl -H "Authorization: Bearer $token" http://127.0.0.1:$port/trigger/script/{code}"""
    CodeBlock(
        label = stringResource(R.string.webhook_example_script),
        code = curlScript,
        clipboard = clipboard,
        scope = scope,
        onCopied = { showCopied = it },
    )

    Spacer(modifier = Modifier.height(8.dp))

    val curlAutomation =
        """curl -H "Authorization: Bearer $token" http://127.0.0.1:$port/trigger/automation/{code}"""
    CodeBlock(
        label = stringResource(R.string.webhook_example_automation),
        code = curlAutomation,
        clipboard = clipboard,
        scope = scope,
        onCopied = { showCopied = it },
    )

    if (showCopied) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.copied_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.webhook_query_param_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CodeBlock(
    label: String,
    code: String,
    clipboard: Clipboard,
    scope: CoroutineScope,
    onCopied: (Boolean) -> Unit,
) {
    var showCopied by remember { mutableStateOf(false) }

    LaunchedEffect(showCopied) {
        if (showCopied) {
            delay(1500)
            showCopied = false
            onCopied(false)
        }
    }

    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(onClick = {
                    scope.launch {
                        val clipData = ClipData.newPlainText("curl_command", code)
                        clipboard.setClipEntry(ClipEntry(clipData))
                        showCopied = true
                        onCopied(true)
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.copy_clipboard_description),
                        tint = if (showCopied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            SelectionContainer {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Webhook Settings")
@Composable
private fun PreviewWebhookSettingsScreen() {
    ScriptRunnerForTermuxTheme {
        WebhookSettingsScreen(
            state =
                WebhookSettingsState(
                    isEnabled = true,
                    isRunning = true,
                    port = 8080,
                    token = "abc123def456ghi789jkl012mno345pq",
                    lanAccess = false,
                ),
            actions =
                WebhookSettingsActions(
                    onBack = {},
                    onToggleEnabled = {},
                    onToggleLanAccess = {},
                    onRegenerateToken = {},
                    onPortChange = {},
                ),
        )
    }
}
