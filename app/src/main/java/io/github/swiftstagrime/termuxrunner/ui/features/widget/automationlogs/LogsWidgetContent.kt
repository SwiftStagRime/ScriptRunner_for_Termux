package io.github.swiftstagrime.termuxrunner.ui.features.widget.automationlogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
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
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationLog
import io.github.swiftstagrime.termuxrunner.ui.preview.WidgetMockData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun LogsWidgetContent(
    logs: List<AutomationLog>,
    automationMap: Map<Int, Automation>
) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(12.dp)
    ) {
        Text(
            text = context.getString(R.string.logs_widget_title),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        )

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (logs.isEmpty()) {
            Text(
                text = context.getString(R.string.logs_no_activity),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp
                )
            )
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(logs.sortedByDescending { it.timestamp }) { log ->
                    val automation = automationMap[log.automationId]
                    val isSuccess = log.exitCode == 0
                    val statusColor = if (isSuccess) GlanceTheme.colors.primary else GlanceTheme.colors.error

                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(GlanceTheme.colors.surfaceVariant)
                                .cornerRadius(6.dp)
                                .padding(6.dp),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Image(
                                provider = ImageProvider(
                                    if (isSuccess) R.drawable.ic_check_circle else R.drawable.ic_close
                                ),
                                contentDescription = null,
                                modifier = GlanceModifier.size(16.dp),
                                colorFilter = ColorFilter.tint(statusColor)
                            )

                            Spacer(modifier = GlanceModifier.width(8.dp))

                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = automation?.label ?: context.getString(R.string.logs_unknown_automation),
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurface,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp
                                    )
                                )

                                val dateStr = try {
                                    SimpleDateFormat("HH:mm, dd/MM", Locale.getDefault()).format(Date(log.timestamp))
                                } catch (e: Exception) {
                                    ""
                                }

                                Text(
                                    text = dateStr,
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurfaceVariant,
                                        fontSize = 9.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Preview(widthDp = 300, heightDp = 250)
@Composable
fun LogsWidgetPreview() {
    GlanceTheme {
        LogsWidgetContent(
            logs = WidgetMockData.logs,
            automationMap = WidgetMockData.automations.associateBy { it.id }
        )
    }
}
























































