package io.github.swiftstagrime.termuxrunner.domain.model

data class AutomationLog(
    val id: Long = 0,
    val automationId: Int,
    val timestamp: Long,
    val exitCode: Int,
    val message: String? = null
)