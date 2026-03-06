package io.github.swiftstagrime.termuxrunner.data.receiver

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dagger.hilt.android.AndroidEntryPoint
import io.github.swiftstagrime.termuxrunner.ui.features.widget.automationlogs.AutomationLogsWidget

@AndroidEntryPoint
class AutomationLogWidgetReceiver : GlanceAppWidgetReceiver(){
    override val glanceAppWidget: GlanceAppWidget = AutomationLogsWidget()
}