package io.github.swiftstagrime.termuxrunner.ui.features.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.model.ScriptExportField

@Composable
fun ScriptExportDialog(
    script: Script,
    onDismiss: () -> Unit,
    onExportRaw: (fileName: String) -> Unit,
    onExportJson: (fileName: String, fields: Set<ScriptExportField>) -> Unit,
) {
    var jsonFormatSelected by remember { mutableStateOf(false) }
    var selectedFields by remember {
        mutableStateOf(
            setOf(ScriptExportField.PRESETS, ScriptExportField.ICON),
        )
    }

    val rawFileName = "${script.name}.${script.fileExtension.ifBlank { "sh" }}"
    val jsonFileName = "${script.name}.json"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                Text(
                    text = stringResource(R.string.export_script_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = script.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.export_script_format_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(12.dp))

                ExportFormatOption(
                    title = stringResource(R.string.export_script_raw),
                    description = stringResource(R.string.export_script_raw_desc),
                    fileName = rawFileName,
                    selected = !jsonFormatSelected,
                    onClick = { jsonFormatSelected = false },
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExportFormatOption(
                    title = stringResource(R.string.export_script_json),
                    description = stringResource(R.string.export_script_json_desc),
                    fileName = jsonFileName,
                    selected = jsonFormatSelected,
                    onClick = { jsonFormatSelected = true },
                )

                if (jsonFormatSelected) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = stringResource(R.string.export_script_include_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            ExportFieldRow(
                                title = stringResource(R.string.export_field_env_vars),
                                checked = ScriptExportField.ENV_VARS in selectedFields,
                                onCheckedChange = { checked ->
                                    selectedFields =
                                        if (checked) {
                                            selectedFields + ScriptExportField.ENV_VARS
                                        } else {
                                            selectedFields - ScriptExportField.ENV_VARS
                                        }
                                },
                            )
                            HorizontalDivider(
                                modifier =
                                    Modifier
                                        .padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            )
                            ExportFieldRow(
                                title = stringResource(R.string.export_field_adb_code),
                                checked = ScriptExportField.ADB_CODE in selectedFields,
                                onCheckedChange = { checked ->
                                    selectedFields =
                                        if (checked) {
                                            selectedFields + ScriptExportField.ADB_CODE
                                        } else {
                                            selectedFields - ScriptExportField.ADB_CODE
                                        }
                                },
                            )
                            HorizontalDivider(
                                modifier =
                                    Modifier
                                        .padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            )
                            ExportFieldRow(
                                title = stringResource(R.string.export_field_presets),
                                checked = ScriptExportField.PRESETS in selectedFields,
                                onCheckedChange = { checked ->
                                    selectedFields =
                                        if (checked) {
                                            selectedFields + ScriptExportField.PRESETS
                                        } else {
                                            selectedFields - ScriptExportField.PRESETS
                                        }
                                },
                            )
                            HorizontalDivider(
                                modifier =
                                    Modifier
                                        .padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            )
                            ExportFieldRow(
                                title = stringResource(R.string.export_field_icon),
                                checked = ScriptExportField.ICON in selectedFields,
                                onCheckedChange = { checked ->
                                    selectedFields =
                                        if (checked) {
                                            selectedFields + ScriptExportField.ICON
                                        } else {
                                            selectedFields - ScriptExportField.ICON
                                        }
                                },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.export_sensitive_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            if (jsonFormatSelected) {
                                onExportJson(jsonFileName, selectedFields)
                            } else {
                                onExportRaw(rawFileName)
                            }
                        },
                    ) {
                        Text(stringResource(R.string.export_button_label))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportFormatOption(
    title: String,
    description: String,
    fileName: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color =
            if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            },
        border =
            BorderStroke(
                1.dp,
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                },
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector =
                    if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint =
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ExportFieldRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
