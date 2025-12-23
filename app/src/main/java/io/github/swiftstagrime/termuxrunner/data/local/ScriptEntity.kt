package io.github.swiftstagrime.termuxrunner.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
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
) {
    fun toDomain(): Script {
        return Script(
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
            iconPath = iconPath
        )
    }
}

fun Script.toEntity(): ScriptEntity {
    return ScriptEntity(
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
        iconPath = iconPath
    )
}