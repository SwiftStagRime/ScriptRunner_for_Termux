package io.github.swiftstagrime.termuxrunner.ui.features.widget.automation

import android.content.Context
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dagger.hilt.android.EntryPointAccessors
import io.github.swiftstagrime.termuxrunner.di.WidgetEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RunAutomationAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val id = parameters[WidgetActionKeys.ID] ?: return

        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val automation = entryPoint.automationRepository().getAutomationById(id) ?: return
        val script = entryPoint.scriptRepository().getScriptById(automation.scriptId) ?: return

        try {
            entryPoint.runScriptUseCase()(
                script = script,
                runtimeArgs = automation.runtimeArgs,
                runtimeEnv = automation.runtimeEnv,
                runtimePrefix = automation.runtimePrefix,
                automationId = automation.id,
            )
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        AutomationWidget().update(context, glanceId)
    }
}
