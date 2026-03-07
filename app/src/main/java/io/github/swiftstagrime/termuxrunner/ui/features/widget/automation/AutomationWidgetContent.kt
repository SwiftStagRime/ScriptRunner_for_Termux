package io.github.swiftstagrime.termuxrunner.ui.features.widget.automation

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.Switch
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.model.Automation
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AutomationWidgetContent(
    automations: List<Automation>,
    scriptMap: Map<Int, Script>,
    maxItems: Int = 10,
    getToggleAction: (Int, Boolean) -> androidx.glance.action.Action,
    getRunAction: (Int) -> androidx.glance.action.Action,
) {
    val context = LocalContext.current
    val activeCount = automations.count { it.isEnabled }

    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.widgetBackground)
                .padding(12.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = context.getString(R.string.automation_widget_title),
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    ),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = context.getString(R.string.automation_active_count, activeCount),
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                    ),
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            items(automations.sortedBy { it.label }.take(maxItems)) { automation ->
                Column(modifier = GlanceModifier.padding(bottom = 8.dp)) {
                    AutomationRow(
                        automation = automation,
                        script = scriptMap[automation.scriptId],
                        onToggleAction = getToggleAction(automation.id, !automation.isEnabled),
                        onRunAction = getRunAction(automation.id),
                    )
                }
            }
        }
    }
}

@Composable
private fun AutomationRow(
    automation: Automation,
    script: Script?,
    onToggleAction: androidx.glance.action.Action,
    onRunAction: androidx.glance.action.Action,
) {
    val context = LocalContext.current
    Row(
        modifier =
            GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(GlanceTheme.colors.surfaceVariant)
                .cornerRadius(8.dp)
                .padding(8.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        val statusColor =
            when (automation.lastExitCode) {
                0 -> GlanceTheme.colors.primary
                null -> GlanceTheme.colors.outline
                else -> GlanceTheme.colors.error
            }

        Box(
            modifier =
                GlanceModifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(statusColor),
        ) {}

        Spacer(modifier = GlanceModifier.width(8.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = automation.label,
                maxLines = 1,
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                    ),
            )
            Text(
                text = script?.name ?: context.getString(R.string.automation_no_script),
                maxLines = 1,
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 10.sp,
                    ),
            )

            if (automation.nextRunTimestamp != null) {
                val timeStr =
                    SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(Date(automation.nextRunTimestamp))
                Text(
                    text = context.getString(R.string.automation_next_run, timeStr),
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 9.sp,
                        ),
                )
            }
        }

        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            Switch(
                checked = automation.isEnabled,
                onCheckedChange = onToggleAction,
                modifier = GlanceModifier.padding(0.dp),
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            Image(
                provider = ImageProvider(R.drawable.ic_play),
                contentDescription = context.getString(R.string.automation_run_now_description),
                modifier =
                    GlanceModifier
                        .size(24.dp)
                        .clickable(onRunAction),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
            )
        }
    }
}
