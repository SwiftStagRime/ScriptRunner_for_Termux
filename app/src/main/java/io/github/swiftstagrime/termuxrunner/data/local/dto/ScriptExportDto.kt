package io.github.swiftstagrime.termuxrunner.data.local.dto

import io.github.swiftstagrime.termuxrunner.domain.model.InteractionMode
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import kotlinx.serialization.Serializable

@Serializable
data class ScriptExportDto(
    val id: Int = 0,
    val name: String,
    val code: String,
    val categoryId: Int? = null,
    val interpreter: String = "",
    val fileExtension: String = "",
    val commandPrefix: String = "",
    val runInBackground: Boolean = false,
    val openNewSession: Boolean = false,
    val executionParams: String = "",
    val envVars: Map<String, String> = emptyMap(),
    val keepSessionOpen: Boolean = false,
    val useHeartbeat: Boolean = false,
    val heartbeatTimeout: Long = 30000,
    val heartbeatInterval: Long = 10000,
    val iconBase64: String? = null,
    val orderIndex: Int = 0,
    val notifyOnResult: Boolean = false,
    val interactionMode: InteractionMode = InteractionMode.NONE,
    val argumentPresets: List<String> = emptyList(),
    val prefixPresets: List<String> = emptyList(),
    val envVarPresets: List<String> = emptyList(),
)

fun Script.toExportDto(base64Icon: String?): ScriptExportDto =
    ScriptExportDto(
        id = id,
        name = name,
        code = code,
        categoryId = categoryId,
        interpreter = interpreter,
        fileExtension = fileExtension,
        commandPrefix = commandPrefix,
        runInBackground = runInBackground,
        openNewSession = openNewSession,
        executionParams = executionParams,
        envVars = envVars,
        keepSessionOpen = keepSessionOpen,
        useHeartbeat = useHeartbeat,
        heartbeatTimeout = heartbeatTimeout,
        heartbeatInterval = heartbeatInterval,
        iconBase64 = base64Icon,
        orderIndex = orderIndex,
        notifyOnResult = notifyOnResult,
        interactionMode = interactionMode,
        argumentPresets = argumentPresets,
        prefixPresets = prefixPresets,
        envVarPresets = envVarPresets,
    )
