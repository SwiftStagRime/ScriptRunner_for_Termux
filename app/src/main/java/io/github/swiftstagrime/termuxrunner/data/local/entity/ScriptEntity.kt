package io.github.swiftstagrime.termuxrunner.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.swiftstagrime.termuxrunner.domain.model.InteractionMode
import io.github.swiftstagrime.termuxrunner.domain.model.Script

@Entity(tableName = "scripts")
data class ScriptEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val code: String,
    val interpreter: String,
    val fileExtension: String = "sh",
    val commandPrefix: String = "",
    val runInBackground: Boolean,
    val openNewSession: Boolean,
    val executionParams: String,
    val iconPath: String?,
    val envVars: Map<String, String>,
    val keepSessionOpen: Boolean,
    @ColumnInfo(defaultValue = "0")
    val useHeartbeat: Boolean = false,
    @ColumnInfo(defaultValue = "30000")
    val heartbeatTimeout: Long = 30000,
    @ColumnInfo(defaultValue = "10000")
    val heartbeatInterval: Long = 10000,
    @ColumnInfo(defaultValue = "NULL")
    val categoryId: Int? = null,
    @ColumnInfo(defaultValue = "0")
    val orderIndex: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val notifyOnResult: Boolean = false,
    @ColumnInfo(defaultValue = "NONE")
    val interactionMode: InteractionMode = InteractionMode.NONE,
    @ColumnInfo(defaultValue = "")
    val argumentPresets: List<String> = emptyList(),
    @ColumnInfo(defaultValue = "")
    val prefixPresets: List<String> = emptyList(),
    @ColumnInfo(defaultValue = "")
    val envVarPresets: List<String> = emptyList(),
) {
    fun toScriptDomain(): Script =
        Script(
            id = id,
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
            iconPath = iconPath,
            useHeartbeat = useHeartbeat,
            heartbeatTimeout = heartbeatTimeout,
            heartbeatInterval = heartbeatInterval,
            categoryId = categoryId,
            orderIndex = orderIndex,
            notifyOnResult = notifyOnResult,
            interactionMode = interactionMode,
            argumentPresets = argumentPresets,
            prefixPresets = prefixPresets,
            envVarPresets = envVarPresets,
        )
}

fun Script.toScriptEntity(): ScriptEntity =
    ScriptEntity(
        id = id,
        name = name,
        code = code,
        interpreter = interpreter,
        fileExtension = fileExtension,
        commandPrefix = commandPrefix,
        runInBackground = runInBackground,
        openNewSession = openNewSession,
        executionParams = executionParams,
        keepSessionOpen = keepSessionOpen,
        envVars = envVars,
        iconPath = iconPath,
        useHeartbeat = useHeartbeat,
        heartbeatTimeout = heartbeatTimeout,
        heartbeatInterval = heartbeatInterval,
        categoryId = categoryId,
        orderIndex = orderIndex,
        notifyOnResult = notifyOnResult,
        interactionMode = interactionMode,
        argumentPresets = argumentPresets,
        prefixPresets = prefixPresets,
        envVarPresets = envVarPresets,
    )
