package io.github.swiftstagrime.termuxrunner.domain.usecase

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.swiftstagrime.termuxrunner.ui.features.widget.automation.AutomationWidget
import io.github.swiftstagrime.termuxrunner.ui.features.widget.automationlogs.AutomationLogsWidget
import io.github.swiftstagrime.termuxrunner.ui.features.widget.script.ScriptWidget
import javax.inject.Inject

class TriggerGlanceScriptUpdateUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke() {
        ScriptWidget().updateAll(context)
    }
}