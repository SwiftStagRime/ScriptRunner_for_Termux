package io.github.swiftstagrime.termuxrunner.domain.usecase

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.swiftstagrime.termuxrunner.ui.features.widget.automation.AutomationWidget
import io.github.swiftstagrime.termuxrunner.ui.features.widget.automationlogs.AutomationLogsWidget
import javax.inject.Inject

class TriggerGlanceAutomationUpdateUseCase
    @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke() {
        AutomationWidget().updateAll(context)
        AutomationLogsWidget().updateAll(context)
    }
}