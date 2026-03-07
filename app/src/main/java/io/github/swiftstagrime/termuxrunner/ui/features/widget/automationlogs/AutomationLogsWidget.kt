package io.github.swiftstagrime.termuxrunner.ui.features.widget.automationlogs

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.material3.ColorProviders
import dagger.hilt.android.EntryPointAccessors
import io.github.swiftstagrime.termuxrunner.di.WidgetEntryPoint
import io.github.swiftstagrime.termuxrunner.ui.theme.pickColorScheme
import kotlinx.coroutines.flow.first

class AutomationLogsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val prefsRepo = entryPoint.userPreferencesRepository()
        val accent = prefsRepo.selectedAccent.first()

        val lightScheme = pickColorScheme(accent, isDark = false, context)
        val darkScheme = pickColorScheme(accent, isDark = true, context)
        val glanceColors = ColorProviders(light = lightScheme, dark = darkScheme)

        provideContent {
            val logs by entryPoint.automationLogRepository().getRecentLogs(5).collectAsState(emptyList())
            val automations by entryPoint.automationRepository().getAllAutomations().collectAsState(emptyList())
            val automationMap = remember(automations) { automations.associateBy { it.id } }

            GlanceTheme(glanceColors) {
                LogsWidgetContent(logs, automationMap)
            }
        }
    }
}
