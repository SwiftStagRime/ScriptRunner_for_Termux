package io.github.swiftstagrime.termuxrunner.domain.repository

import io.github.swiftstagrime.termuxrunner.domain.model.Script

interface MonitoringRepository {
    fun startMonitoring(script: Script)

    fun stopMonitoring()

    fun hasNotificationPermission(): Boolean
}
