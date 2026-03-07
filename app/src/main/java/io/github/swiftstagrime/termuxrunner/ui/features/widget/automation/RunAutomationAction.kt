package io.github.swiftstagrime.termuxrunner.ui.features.widget.automation

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dagger.hilt.android.EntryPointAccessors
import io.github.swiftstagrime.termuxrunner.di.WidgetEntryPoint

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

        entryPoint.runScriptUseCase()(
            script = script.copy(notifyOnResult = true),
            runtimeArgs = automation.runtimeArgs,
            runtimeEnv = automation.runtimeEnv,
            runtimePrefix = automation.runtimePrefix,
            automationId = automation.id,
        )

        AutomationWidget().update(context, glanceId)
    }
}
