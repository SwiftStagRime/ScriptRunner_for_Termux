package io.github.swiftstagrime.termuxrunner.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.model.InteractionMode
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.ui.preview.multiChoiceScript
import io.github.swiftstagrime.termuxrunner.ui.preview.textInputScript
import io.github.swiftstagrime.termuxrunner.ui.theme.ScriptRunnerForTermuxTheme

@Composable
fun ScriptRuntimePromptDialog(
    script: Script,
    onDismiss: () -> Unit,
    onConfirm: (runtimeArgs: String, runtimePrefix: String, runtimeEnv: Map<String, String>) -> Unit,
) {
    var runtimePrefix by rememberSaveable { mutableStateOf(script.commandPrefix) }
    var runtimeArgs by rememberSaveable { mutableStateOf(script.executionParams) }

    val selectedMultiOptions =
        rememberSaveable(saver = snapshotStateListSaver<String>()) {
            mutableStateListOf()
        }

    val selectedEnvPresets =
        rememberSaveable(saver = snapshotStateListSaver()) {
            mutableStateListOf<String>().apply {
                addAll(script.envVarPresets.map { it.split("=")[0] })
            }
        }

    val multiModeEnvMap =
        rememberSaveable(saver = mapStateSaver()) {
            val map = mutableStateMapOf<String, String>()
            script.envVarPresets.forEach { raw ->
                val parts = raw.split("=", limit = 2)
                val key = parts.getOrNull(0) ?: ""
                val defaultValue = parts.getOrNull(1) ?: ""
                map[key] = defaultValue
            }
            map
        }

    val textModeEnvList =
        rememberSaveable(saver = listPairSaver()) {
            script.envVarPresets
                .map { raw ->
                    val parts = raw.split("=", limit = 2)
                    (parts.getOrNull(0) ?: "") to (parts.getOrNull(1) ?: "")
                }.toMutableStateList()
        }

    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
                    .imePadding(),
            shape = RoundedCornerShape(24.dp),
            colors =
                CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                DialogHeader(scriptName = script.name)

                when (script.interactionMode) {
                    InteractionMode.TEXT_INPUT -> {
                        TextInputSection(
                            runtimePrefix = runtimePrefix,
                            onPrefixChange = { runtimePrefix = it },
                            runtimeArgs = runtimeArgs,
                            onArgsChange = { runtimeArgs = it },
                            envList = textModeEnvList,
                        )
                    }
                    InteractionMode.MULTI_CHOICE -> {
                        MultiChoiceSection(
                            script = script,
                            currentPrefix = runtimePrefix,
                            onPrefixChange = { runtimePrefix = it },
                            selectedOptions = selectedMultiOptions,
                            envMap = multiModeEnvMap,
                            selectedEnvPresets = selectedEnvPresets,
                        )
                    }

                    InteractionMode.NONE -> {}
                }

                ActionButtons(
                    onDismiss = onDismiss,
                    onRun = {
                        val finalArgs =
                            if (script.interactionMode == InteractionMode.MULTI_CHOICE) {
                                selectedMultiOptions.joinToString(" ")
                            } else {
                                runtimeArgs
                            }

                        val finalEnv =
                            if (script.interactionMode == InteractionMode.MULTI_CHOICE) {
                                multiModeEnvMap.filterKeys { it in selectedEnvPresets }.toMap()
                            } else {
                                textModeEnvList.filter { it.first.isNotBlank() }.toMap()
                            }

                        onConfirm(finalArgs, runtimePrefix, finalEnv)
                    },
                )
            }
        }
    }
}

@Composable
private fun DialogHeader(scriptName: String) {
    Text(
        text = stringResource(R.string.title_run_options, scriptName),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun TextInputSection(
    runtimePrefix: String,
    onPrefixChange: (String) -> Unit,
    runtimeArgs: String,
    onArgsChange: (String) -> Unit,
    envList: SnapshotStateList<Pair<String, String>>,
) {
    StyledTextField(
        value = runtimePrefix,
        onValueChange = onPrefixChange,
        label = stringResource(R.string.label_choose_prefix),
        modifier = Modifier.fillMaxWidth(),
    )

    StyledTextField(
        value = runtimeArgs,
        onValueChange = onArgsChange,
        label = stringResource(R.string.label_enter_arguments),
        modifier = Modifier.fillMaxWidth(),
    )

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

    Text(
        text = stringResource(R.string.section_env_vars),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
    )

    envList.forEachIndexed { index, (key, value) ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StyledTextField(
                value = key,
                onValueChange = { envList[index] = it to value },
                label = stringResource(R.string.label_key),
                modifier = Modifier.weight(1f),
            )

            Text("=", modifier = Modifier.padding(horizontal = 8.dp))

            StyledTextField(
                value = value,
                onValueChange = { envList[index] = key to it },
                label = stringResource(R.string.label_value),
                modifier = Modifier.weight(1.2f),
            )

            IconButton(onClick = { envList.removeAt(index) }) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    TextButton(onClick = { envList.add("" to "") }) {
        Icon(Icons.Default.Add, null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.btn_add_variable))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MultiChoiceSection(
    script: Script,
    currentPrefix: String,
    onPrefixChange: (String) -> Unit,
    selectedOptions: SnapshotStateList<String>,
    envMap: SnapshotStateMap<String, String>,
    selectedEnvPresets: SnapshotStateList<String>,
) {
    if (script.prefixPresets.isNotEmpty()) {
        Text(stringResource(R.string.label_choose_prefix), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            script.prefixPresets.forEach { prefix ->
                FilterChip(
                    selected = currentPrefix == prefix,
                    onClick = { onPrefixChange(prefix) },
                    label = { Text(prefix.ifBlank { "default" }) },
                )
            }
        }
    }

    Text(stringResource(R.string.label_select_options), style = MaterialTheme.typography.labelLarge)
    script.argumentPresets.forEach { option ->
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (selectedOptions.contains(option)) {
                            selectedOptions.remove(option)
                        } else {
                            selectedOptions.add(option)
                        }
                    }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = selectedOptions.contains(option), onCheckedChange = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(option)
        }
    }

    if (script.envVarPresets.isNotEmpty()) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(stringResource(R.string.section_env_vars), style = MaterialTheme.typography.labelLarge)

        script.envVarPresets.forEach { raw ->
            val parts = raw.split("=", limit = 2)
            val keyLabel = parts.getOrNull(0) ?: raw
            val isSelected = selectedEnvPresets.contains(keyLabel)

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { checked ->
                        if (checked) {
                            selectedEnvPresets.add(keyLabel)
                        } else {
                            selectedEnvPresets.remove(keyLabel)
                        }
                    },
                )

                StyledTextField(
                    value = envMap[keyLabel] ?: "",
                    onValueChange = { envMap[keyLabel] = it },
                    label = keyLabel,
                    modifier = Modifier.weight(1f),
                    enabled = isSelected,
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(
    onDismiss: () -> Unit,
    onRun: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onRun,
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Default.PlayArrow, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.btn_run))
        }
    }
}

fun <T : Any> snapshotStateListSaver() =
    Saver<SnapshotStateList<T>, ArrayList<T>>(
        save = { ArrayList(it) },
        restore = { it.toMutableStateList() },
    )

private fun listPairSaver() =
    Saver<SnapshotStateList<Pair<String, String>>, ArrayList<String>>(
        save = {
            val flatList = ArrayList<String>()
            it.forEach { pair ->
                flatList.add(pair.first)
                flatList.add(pair.second)
            }
            flatList
        },
        restore = { flatList ->
            val list = mutableStateListOf<Pair<String, String>>()
            for (i in flatList.indices step 2) {
                if (i + 1 < flatList.size) {
                    list.add(flatList[i] to flatList[i + 1])
                }
            }
            list
        },
    )

private fun mapStateSaver() =
    Saver<SnapshotStateMap<String, String>, HashMap<String, String>>(
        save = { HashMap(it) },
        restore = {
            val map = mutableStateMapOf<String, String>()
            it.forEach { (k, v) -> map[k] = v }
            map
        },
    )

@Preview(name = "Text Input Mode - Light", showBackground = true)
@Composable
private fun PreviewRuntimeDialogText() {
    ScriptRunnerForTermuxTheme {
        Surface {
            ScriptRuntimePromptDialog(
                script = textInputScript,
                onDismiss = {},
                onConfirm = { _, _, _ -> },
            )
        }
    }
}

@Preview(
    name = "Multi Choice Mode - Night",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PreviewRuntimeDialogMulti() {
    ScriptRunnerForTermuxTheme {
        Surface {
            ScriptRuntimePromptDialog(
                script = multiChoiceScript,
                onDismiss = {},
                onConfirm = { _, _, _ -> },
            )
        }
    }
}
