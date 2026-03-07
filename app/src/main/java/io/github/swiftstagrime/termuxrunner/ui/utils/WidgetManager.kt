package io.github.swiftstagrime.termuxrunner.ui.utils

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.swiftstagrime.termuxrunner.ui.features.widget.automation.AutomationWidget
import io.github.swiftstagrime.termuxrunner.ui.features.widget.automationlogs.AutomationLogsWidget
import io.github.swiftstagrime.termuxrunner.ui.features.widget.script.ScriptWidget
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        suspend fun updateAllWidgets() {
            AutomationWidget().updateAll(context)
            ScriptWidget().updateAll(context)
            AutomationLogsWidget().updateAll(context)
        }

        suspend fun updateAutomationWidget() {
            AutomationWidget().updateAll(context)
        }

        suspend fun updateLogsWidget() {
            AutomationLogsWidget().updateAll(context)
        }

        suspend fun updateScriptsWidget() {
            ScriptWidget().updateAll(context)
        }
    }
