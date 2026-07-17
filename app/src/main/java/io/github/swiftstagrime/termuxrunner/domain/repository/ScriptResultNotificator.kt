package io.github.swiftstagrime.termuxrunner.domain.repository

interface ScriptResultNotificator {
    fun showResultNotification(
        scriptId: Int,
        name: String,
        exitCode: Int,
        internalError: String?,
    )
}
