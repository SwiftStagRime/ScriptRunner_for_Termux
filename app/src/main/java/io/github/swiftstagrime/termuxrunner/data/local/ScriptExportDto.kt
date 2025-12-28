package io.github.swiftstagrime.termuxrunner.data.local

import io.github.swiftstagrime.termuxrunner.domain.model.Script
import kotlinx.serialization.Serializable

@Serializable
data class ScriptExportDto(
    val name: String,
    val code: String,
    val interpreter: String,
    val fileExtension: String,
    val commandPrefix: String,
    val runInBackground: Boolean,
    val openNewSession: Boolean,
    val executionParams: String,
    val envVars: Map<String, String>,
    val keepSessionOpen: Boolean,
    val iconBase64: String? = null
)

fun Script.toExportDto(base64Icon: String?): ScriptExportDto {
    return ScriptExportDto(
        name = name,
        code = code,
        interpreter = interpreter,
        fileExtension = fileExtension,
        commandPrefix = commandPrefix,
        runInBackground = runInBackground,
        openNewSession = openNewSession,
        executionParams = executionParams,
        envVars = envVars,
        keepSessionOpen = keepSessionOpen,
        iconBase64 = base64Icon
    )
}