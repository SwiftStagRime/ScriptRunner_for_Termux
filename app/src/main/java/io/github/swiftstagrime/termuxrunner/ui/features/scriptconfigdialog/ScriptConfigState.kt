package io.github.swiftstagrime.termuxrunner.ui.features.scriptconfigdialog

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import io.github.swiftstagrime.termuxrunner.domain.model.Script

private const val MS_TO_S = 1000L

class ScriptConfigState(
    script: Script,
) {
    var name by mutableStateOf(script.name)
    var nameError by mutableStateOf(false)
    var interpreter by mutableStateOf(script.interpreter)
    var fileExtension by mutableStateOf(script.fileExtension)
    var commandPrefix by mutableStateOf(script.commandPrefix)
    var executionParams by mutableStateOf(script.executionParams)
    var iconPath by mutableStateOf(script.iconPath)
    var selectedCategoryId by mutableStateOf(script.categoryId)
    var useHeartbeat by mutableStateOf(script.useHeartbeat)
    var heartbeatInterval by mutableStateOf((script.heartbeatInterval / MS_TO_S).toString())
    var heartbeatTimeout by mutableStateOf((script.heartbeatTimeout / MS_TO_S).toString())
    var runInBackground by mutableStateOf(script.runInBackground)
    var keepOpen by mutableStateOf(script.keepSessionOpen)
    var interactionMode by mutableStateOf(script.interactionMode)
    var showAddCategoryDialog by mutableStateOf(false)
    var notifyOnResult by mutableStateOf(script.notifyOnResult)
    val envVars =
        script.envVars.entries
            .map { it.key to it.value }
            .toMutableStateList()
    val argumentPresets = script.argumentPresets.toMutableStateList()
    val prefixPresets = script.prefixPresets.toMutableStateList()
    val envVarPresets = script.envVarPresets.toMutableStateList()

    fun validate(): Boolean {
        if (name.isBlank()) {
            nameError = true
            return false
        }
        return true
    }

    fun toScript(original: Script): Script =
        original.copy(
            name = name,
            interpreter = interpreter,
            fileExtension = fileExtension,
            commandPrefix = commandPrefix,
            iconPath = iconPath,
            categoryId = selectedCategoryId,
            useHeartbeat = useHeartbeat,
            executionParams = executionParams,
            heartbeatInterval = heartbeatInterval.toLongOrNull()?.times(MS_TO_S) ?: original.heartbeatInterval,
            heartbeatTimeout = heartbeatTimeout.toLongOrNull()?.times(MS_TO_S) ?: original.heartbeatTimeout,
            runInBackground = runInBackground,
            keepSessionOpen = keepOpen,
            interactionMode = interactionMode,
            notifyOnResult = notifyOnResult,
            envVars = envVars.filter { it.first.isNotBlank() }.toMap(),
            argumentPresets = argumentPresets.toList(),
            prefixPresets = prefixPresets.toList(),
            envVarPresets = envVarPresets.toList(),
        )
}
