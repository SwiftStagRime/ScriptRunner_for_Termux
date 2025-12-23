package io.github.swiftstagrime.termuxrunner.domain.repository

interface TermuxRepository {
    fun isTermuxInstalled(): Boolean
    fun isPermissionGranted(): Boolean

    fun requestTermuxOverlay()

    fun runCommand(
        command: String,
        runInBackground: Boolean,
        sessionAction: String
    )

    fun isTermuxBatteryOptimized(): Boolean
}