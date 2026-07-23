package io.github.swiftstagrime.termuxrunner.domain.model

enum class ExecutionSource {
    MANUAL,
    AUTOMATION,
    TILE,
    WIDGET,
    SHORTCUT,
}

data class ScriptExecution(
    val id: Long = 0,
    val scriptId: Int,
    val scriptName: String,
    val timestamp: Long,
    val exitCode: Int,
    val durationMs: Long? = null,
    val runtimeArgs: String? = null,
    val source: ExecutionSource = ExecutionSource.MANUAL,
    val errorMessage: String? = null,
) {
    val isSuccess: Boolean
        get() = exitCode == 0

    val statusText: String
        get() = if (isSuccess) "Success" else "Failed (code $exitCode)"
}
