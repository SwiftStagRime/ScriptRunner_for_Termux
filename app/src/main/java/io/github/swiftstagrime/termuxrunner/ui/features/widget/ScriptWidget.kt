package io.github.swiftstagrime.termuxrunner.ui.features.widget

import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ListItem
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import dagger.hilt.android.EntryPointAccessors
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.di.WidgetEntryPoint
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.ui.theme.AppTheme
import kotlinx.coroutines.flow.first
import androidx.glance.material3.ColorProviders
import io.github.swiftstagrime.termuxrunner.ui.theme.pickColorScheme

class ScriptWidget : GlanceAppWidget() {
    companion object {
        val ScriptsListKey = stringPreferencesKey("selected_scripts_ids")
        val ScriptIdActionKey = ActionParameters.Key<Int>("script_id")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
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
            val ids = remember(prefs[ScriptsListKey]) {
                prefs[ScriptsListKey]?.split(",")?.filter { it.isNotEmpty() }?.mapNotNull { it.toIntOrNull() } ?: emptyList()
            }

            val scriptsState = produceState(initialValue = emptyList(), ids) {
                value = ids.mapNotNull { scriptRepo.getScriptById(it) }
            }

            GlanceTheme(colors = glanceColors) {
                ScriptWidgetContent(scriptsState.value, appWidgetId)
            }
        }
    }

}