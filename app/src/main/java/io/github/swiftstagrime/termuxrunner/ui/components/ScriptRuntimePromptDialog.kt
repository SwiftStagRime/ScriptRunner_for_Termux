package io.github.swiftstagrime.termuxrunner.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.model.InteractionMode
import io.github.swiftstagrime.termuxrunner.domain.model.Script

@Composable
fun ScriptRuntimePromptDialog(
    script: Script,
    onDismiss: () -> Unit,
    onConfirm: (runtimeArgs: String, runtimePrefix: String, runtimeEnv: Map<String, String>) -> Unit
) {
    var runtimePrefix by remember { mutableStateOf(script.commandPrefix) }
    var runtimeArgs by remember { mutableStateOf(script.executionParams) }

    val selectedMultiOptions = remember { mutableStateListOf<String>() }
    val multiModeEnvMap = remember {
        mutableStateMapOf<String, String>().apply {
            script.envVarPresets.forEach { key -> put(key, "") }
        }
    }

    val textModeEnvList = remember {
        script.envVarPresets.map { it to "" }.toMutableStateList()
    }

    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
                .imePadding(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.title_run_options, script.name),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                if (script.interactionMode == InteractionMode.TEXT_INPUT) {

                    StyledTextField(
                        value = runtimePrefix,
                        onValueChange = { runtimePrefix = it },
                        label = stringResource(R.string.label_choose_prefix),
                        modifier = Modifier.fillMaxWidth()
                    )

                    StyledTextField(
                        value = runtimeArgs,
                        onValueChange = { runtimeArgs = it },
                        label = stringResource(R.string.label_enter_arguments),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = stringResource(R.string.section_env_vars),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    textModeEnvList.forEachIndexed { index, (key, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StyledTextField(
                                value = key,
                                onValueChange = { textModeEnvList[index] = it to value },
                                label = stringResource(R.string.label_key),
                                modifier = Modifier.weight(1f)
                            )
                            StyledTextField(
                                value = value,
                                onValueChange = { textModeEnvList[index] = key to it },
                                label = stringResource(R.string.label_value),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { textModeEnvList.removeAt(index) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    TextButton(onClick = { textModeEnvList.add("" to "") }) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_add_variable))
                    }

                } else if (script.interactionMode == InteractionMode.MULTI_CHOICE) {

                    if (script.prefixPresets.isNotEmpty()) {
                        Text(
                            stringResource(R.string.label_choose_prefix),
                            style = MaterialTheme.typography.labelLarge
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            script.prefixPresets.forEach { prefix ->
                                FilterChip(
                                    selected = runtimePrefix == prefix,
                                    onClick = { runtimePrefix = prefix },
                                    label = { Text(prefix.ifBlank { "default" }) }
                                )
                            }
                        }
                    }

                    Text(
                        stringResource(R.string.label_select_options),
                        style = MaterialTheme.typography.labelLarge
                    )
                    script.argumentPresets.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectedMultiOptions.contains(option)) selectedMultiOptions.remove(
                                        option
                                    )
                                    else selectedMultiOptions.add(option)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedMultiOptions.contains(option),
                                onCheckedChange = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(option)
                        }
                    }

                    if (script.envVarPresets.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            stringResource(R.string.section_env_vars),
                            style = MaterialTheme.typography.labelLarge
                        )
                        script.envVarPresets.forEach { key ->
                            StyledTextField(
                                value = multiModeEnvMap[key] ?: "",
                                onValueChange = { multiModeEnvMap[key] = it },
                                label = key,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalArgs =
                                if (script.interactionMode == InteractionMode.MULTI_CHOICE) {
                                    selectedMultiOptions.joinToString(" ")
                                } else {
                                    runtimeArgs
                                }

                            val finalEnv =
                                if (script.interactionMode == InteractionMode.MULTI_CHOICE) {
                                    multiModeEnvMap.toMap()
                                } else {
                                    textModeEnvList.filter { it.first.isNotBlank() }.toMap()
                                }

                            onConfirm(finalArgs, runtimePrefix, finalEnv)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_run))
                    }
                }
            }
        }
    }
}