package io.github.swiftstagrime.termuxrunner.ui.preview

import io.github.swiftstagrime.termuxrunner.domain.model.Automation
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationLog
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import io.github.swiftstagrime.termuxrunner.domain.model.Script

object WidgetMockData {
    val scripts = listOf(
        Script(id = 1, name = "Daily Backup", code = "tar -czf backup.tar.gz /data"),
        Script(id = 2, name = "Server Check", code = "curl -I https://google.com"),
        Script(id = 3, name = "Clean Temp", code = "rm -rf /tmp/*")
    )

    val automations = listOf(
        Automation(
            id = 1,
            scriptId = 1,
            label = "Backup Pro",
            type = AutomationType.PERIODIC,
            isEnabled = true,
            lastExitCode = 0,
            nextRunTimestamp = System.currentTimeMillis() + 3600000,
            scheduledTimestamp = 0,
            intervalMillis = 0,
            daysOfWeek = emptyList(),
            lastRunTimestamp = null
        ),
        Automation(
            id = 2, scriptId = 2, label = "Health Check",
            type = AutomationType.WEEKLY, isEnabled = false,
            lastExitCode = 1, nextRunTimestamp = null,
            scheduledTimestamp = 0, intervalMillis = 0, daysOfWeek = emptyList(), lastRunTimestamp = null
        )
    )

    val logs = listOf(
        AutomationLog(
            id = 1,
            automationId = 1,
            timestamp = System.currentTimeMillis(),
            exitCode = 0
        ),
        AutomationLog(id = 2, automationId = 2, timestamp = System.currentTimeMillis() - 86400000, exitCode = 1),
        AutomationLog(id = 3, automationId = 1, timestamp = System.currentTimeMillis() - 172800000, exitCode = 0)
    )
}