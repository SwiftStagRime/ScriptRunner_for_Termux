package io.github.swiftstagrime.termuxrunner.ui.features.widget.automation

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dagger.hilt.android.EntryPointAccessors
import io.github.swiftstagrime.termuxrunner.di.WidgetEntryPoint

class ToggleAutomationAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val id = parameters[WidgetActionKeys.ID] ?: return
        val enabled = parameters[WidgetActionKeys.ENABLED] ?: return

        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)

        entryPoint.automationRepository().toggleAutomation(id, enabled)

        AutomationWidget().update(context, glanceId)
    }
}
