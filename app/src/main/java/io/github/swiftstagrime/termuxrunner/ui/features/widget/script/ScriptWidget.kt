package io.github.swiftstagrime.termuxrunner.ui.features.widget.script

import android.content.Context
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.material3.ColorProviders
import dagger.hilt.android.EntryPointAccessors
import io.github.swiftstagrime.termuxrunner.di.WidgetEntryPoint
import io.github.swiftstagrime.termuxrunner.ui.theme.pickColorScheme
import kotlinx.coroutines.flow.first

class ScriptWidget : GlanceAppWidget() {
    companion object {
        val ScriptsListKey = stringPreferencesKey("selected_scripts_ids")
        val ScriptIdActionKey = ActionParameters.Key<Int>("script_id")
    }

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val scriptRepo = entryPoint.scriptRepository()
        val prefsRepo = entryPoint.userPreferencesRepository()

        val accent = prefsRepo.selectedAccent.first()

        val lightScheme = pickColorScheme(accent, isDark = false, context)
        val darkScheme = pickColorScheme(accent, isDark = true, context)
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val glanceColors = ColorProviders(light = lightScheme, dark = darkScheme)

        provideContent {
            val prefs = currentState<Preferences>()
            val ids =
                remember(prefs[ScriptsListKey]) {
                    prefs[ScriptsListKey]?.split(",")?.filter { it.isNotEmpty() }?.mapNotNull { it.toIntOrNull() } ?: emptyList()
                }

            val scriptsState =
                produceState(initialValue = emptyList(), ids) {
                    value = ids.mapNotNull { scriptRepo.getScriptById(it) }
                }

            GlanceTheme(colors = glanceColors) {
                ScriptWidgetContent(scriptsState.value, appWidgetId)
            }
        }
    }
}
