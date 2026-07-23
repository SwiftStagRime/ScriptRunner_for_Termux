package io.github.swiftstagrime.termuxrunner.domain.repository

import io.github.swiftstagrime.termuxrunner.domain.model.NotificationAction

interface ScriptResultNotificator {
    fun showResultNotification(
        scriptId: Int,
        name: String,
        exitCode: Int,
        internalError: String?,
        actions: List<NotificationAction> = emptyList(),
    )
}
