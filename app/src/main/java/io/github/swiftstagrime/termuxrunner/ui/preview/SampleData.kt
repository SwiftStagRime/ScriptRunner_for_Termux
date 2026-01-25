package io.github.swiftstagrime.termuxrunner.ui.preview

import android.graphics.Color
import io.github.swiftstagrime.termuxrunner.domain.model.Automation
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import io.github.swiftstagrime.termuxrunner.domain.model.Category
import io.github.swiftstagrime.termuxrunner.domain.model.InteractionMode
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.ui.features.automation.AutomationUiItem
import io.github.swiftstagrime.termuxrunner.ui.features.home.HomeActions

val sampleScripts =
    listOf(
        Script(
            id = 1,
            name = "Update System",
            code = "pkg update && pkg upgrade -y",
            interpreter = "bash",
            executionParams = "",
            runInBackground = false,
        ),
        Script(
            id = 2,
            name = "Python Server",
            code = "print('Starting server...')\nimport http.server",
            interpreter = "python",
            executionParams = "-m http.server 8080",
            runInBackground = true,
        ),
    )
val configSampleScript =
    Script(
        id = 1,
        name = "Python API Server",
        code = "print('hello')",
        interpreter = "python",
        executionParams = "-m http.server",
        runInBackground = false,
        keepSessionOpen = true,
        envVars =
            mapOf(
                "PORT" to "8080",
                "API_KEY" to "secret_123",
                "DB_HOST" to "localhost",
            ),
        iconPath = null,
    )

val mockAutomations =
    listOf(
        AutomationUiItem(
            automation =
                Automation(
                    id = 1,
                    scriptId = 1,
                    label = "Daily Database Backup",
                    type = AutomationType.PERIODIC,
                    scheduledTimestamp = System.currentTimeMillis(),
                    intervalMillis = 86400000,
                    daysOfWeek = emptyList(),
                    isEnabled = true,
                    runIfMissed = true,
                    lastRunTimestamp = System.currentTimeMillis() - 3600000,
                    lastExitCode = 0,
                    nextRunTimestamp = System.currentTimeMillis() + 7200000,
                    runtimeArgs = null,
                    runtimeEnv = emptyMap(),
                    runtimePrefix = null,
                ),
            scriptName = "backup_db.sh",
            scriptIconPath = null,
            nextRunText = "Next run: In 2 hours",
            lastRunText = "Last run: 1 hour ago (Success)",
            statusColor = Color.GREEN,
        ),
        AutomationUiItem(
            automation =
                Automation(
                    id = 2,
                    scriptId = 2,
                    label = "Weekly Health Check",
                    type = AutomationType.WEEKLY,
                    scheduledTimestamp = System.currentTimeMillis(),
                    intervalMillis = 0,
                    daysOfWeek = listOf(2, 4),
                    isEnabled = false,
                    runIfMissed = true,
                    lastRunTimestamp = System.currentTimeMillis() - 172800000,
                    lastExitCode = 1,
                    nextRunTimestamp = System.currentTimeMillis() + 43200000,
                    runtimeArgs = "--verbose",
                    runtimeEnv = emptyMap(),
                    runtimePrefix = null,
                ),
            scriptName = "system_check.py",
            scriptIconPath = null,
            nextRunText = "Next run: In 12 hours",
            lastRunText = "Last run: 2 days ago (Failed: 1)",
            statusColor = Color.RED,
        ),
    )

val stubHomeActions =
    HomeActions(
        onSearchQueryChange = {},
        onOpenConfig = {},
        onDismissConfig = {},
        onAddClick = {},
        onSettingsClick = {},
        onScriptCodeClick = {},
        onRunClick = {},
        onDeleteScript = {},
        onCreateShortcutClick = {},
        onUpdateScript = {},
        onHeartbeatToggle = {},
        onRequestBatteryUnrestricted = {},
        onRequestNotificationPermission = {},
        onProcessImage = { null },
        onCategorySelect = {},
        onSortOptionChange = {},
        onAddNewCategory = {},
        onDeleteCategory = {},
        onMove = { _, _ -> },
        onTileSettingsClick = {},
        onNavigateToAutomation = {},
    )

val sampleCategories =
    listOf(
        Category(id = 1, name = "Automation", orderIndex = 0),
        Category(id = 2, name = "Network Tools", orderIndex = 1),
        Category(id = 3, name = "System Maintenance", orderIndex = 2),
        Category(id = 4, name = "Python Scripts", orderIndex = 3),
    )

val textInputScript =
    Script(
        id = 1,
        code = "print('hello')",
        name = "Custom Deployment",
        interactionMode = InteractionMode.TEXT_INPUT,
        commandPrefix = "sudo",
        executionParams = "--verbose --port 8080",
        envVarPresets = listOf("API_KEY", "DB_PASSWORD"),
    )

val multiChoiceScript =
    Script(
        id = 1,
        code = "print('hello')",
        name = "System Cleaner",
        interactionMode = InteractionMode.MULTI_CHOICE,
        prefixPresets = listOf("", "tsu", "sudo"),
        argumentPresets = listOf("--cache", "--temp-files", "--logs", "--old-kernels"),
        envVarPresets = listOf("LOG_LEVEL", "DRY_RUN"),
    )
