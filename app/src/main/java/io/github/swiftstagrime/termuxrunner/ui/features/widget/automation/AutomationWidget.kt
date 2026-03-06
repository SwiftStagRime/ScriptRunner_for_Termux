package io.github.swiftstagrime.termuxrunner.ui.features.widget.automation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.material3.ColorProviders
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import dagger.hilt.android.EntryPointAccessors
import io.github.swiftstagrime.termuxrunner.di.WidgetEntryPoint
import io.github.swiftstagrime.termuxrunner.ui.features.widget.automation.ToggleAutomationAction
import io.github.swiftstagrime.termuxrunner.ui.preview.WidgetMockData
import io.github.swiftstagrime.termuxrunner.ui.theme.pickColorScheme
import kotlinx.coroutines.flow.first

object WidgetActionKeys {
    val ID = ActionParameters.Key<Int>("automation_id")
    val ENABLED = ActionParameters.Key<Boolean>("enabled")
}

class AutomationWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val prefsRepo = entryPoint.userPreferencesRepository()
        val accent = prefsRepo.selectedAccent.first()

        val lightScheme = pickColorScheme(accent, isDark = false, context)
        val darkScheme = pickColorScheme(accent, isDark = true, context)
        val glanceColors = ColorProviders(light = lightScheme, dark = darkScheme)

        provideContent {
            val automations by entryPoint.automationRepository().getAllAutomations().collectAsState(emptyList())
            val scripts by entryPoint.scriptRepository().getAllScripts().collectAsState(emptyList())
            val scriptMap = remember(scripts) { scripts.associateBy { it.id } }

            GlanceTheme(glanceColors) {
                AutomationWidgetContent(
                    automations = automations,
                    scriptMap = scriptMap,
                    getToggleAction = { automationId, nextState ->
                        actionRunCallback<ToggleAutomationAction>(
                            actionParametersOf(
                                WidgetActionKeys.ID to automationId,
                                WidgetActionKeys.ENABLED to nextState
                            )
                        )
                    },
                    getRunAction = { automationId ->
                        actionRunCallback<RunAutomationAction>(
                            actionParametersOf(WidgetActionKeys.ID to automationId)
                        )
                    }
                )
            }
        }
    }
}