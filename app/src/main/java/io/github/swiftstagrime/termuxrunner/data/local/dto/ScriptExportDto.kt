package io.github.swiftstagrime.termuxrunner.data.local.dto

import io.github.swiftstagrime.termuxrunner.data.local.entity.ScriptEntity
import io.github.swiftstagrime.termuxrunner.domain.model.ForegroundSessionBehavior
import io.github.swiftstagrime.termuxrunner.domain.model.InteractionMode
import io.github.swiftstagrime.termuxrunner.domain.model.NotificationAction
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import kotlinx.serialization.Serializable

@Serializable
data class ScriptExportDto(
    val id: Int = 0,
    val name: String,
    val codePages: List<String> = emptyList(),
    val code: String? = null,
    val pageNames: List<String> = emptyList(),
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
    val adbCode: String? = null,
    val notificationActions: List<NotificationAction> = emptyList(),
    val foregroundSessionBehavior: ForegroundSessionBehavior = ForegroundSessionBehavior.KEEP_OPEN,
    val reuseSession: Boolean = false,
)

fun Script.toExportDto(base64Icon: String?): ScriptExportDto =
    ScriptExportDto(
        id = id,
        name = name,
        codePages = codePages,
        pageNames = pageNames,
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
        adbCode = adbCode,
        notificationActions = notificationActions,
        foregroundSessionBehavior = foregroundSessionBehavior,
        reuseSession = reuseSession,
    )

fun ScriptExportDto.toEntity(
    newIconPath: String?,
    mappedCategoryId: Int?,
): ScriptEntity {
    val migratedCodePages =
        when {
            codePages.isNotEmpty() -> codePages
            !code.isNullOrEmpty() -> listOf(code)
            else -> listOf("")
        }

    val migratedPageNames =
        if (pageNames.size == migratedCodePages.size) {
            pageNames
        } else {
            migratedCodePages.mapIndexed { index, _ -> if (index == 0) "Main" else "Page ${index + 1}" }
        }

    return ScriptEntity(
        name = name,
        codePages = migratedCodePages,
        pageNames = migratedPageNames,
        interactionMode = interactionMode,
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
        argumentPresets = argumentPresets,
        prefixPresets = prefixPresets,
        envVarPresets = envVarPresets,
        iconPath = newIconPath,
        orderIndex = orderIndex,
        notifyOnResult = notifyOnResult,
        categoryId = mappedCategoryId,
        adbCode = adbCode,
        notificationActions = notificationActions,
        foregroundSessionBehavior = foregroundSessionBehavior,
        reuseSession = reuseSession,
    )
}
