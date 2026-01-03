package io.github.swiftstagrime.termuxrunner.data.local.dto

import io.github.swiftstagrime.termuxrunner.domain.model.Script
import kotlinx.serialization.Serializable

@Serializable
data class ScriptExportDto(
    val name: String,
    val code: String,
    val categoryId: Int? = null,
    val interpreter: String,
    val fileExtension: String,
    val commandPrefix: String,
    val runInBackground: Boolean,
    val openNewSession: Boolean,
    val executionParams: String,
    val envVars: Map<String, String>,
    val keepSessionOpen: Boolean,
    val useHeartbeat: Boolean = false,
    val heartbeatTimeout: Long = 30000,
    val heartbeatInterval: Long = 10000,
    val iconBase64: String? = null,
    val orderIndex: Int = 0,
    val notifyOnResult: Boolean = false
)

fun Script.toExportDto(base64Icon: String?): ScriptExportDto {
    return ScriptExportDto(
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
        notifyOnResult = notifyOnResult
    )
}