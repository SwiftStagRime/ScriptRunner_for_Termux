package io.github.swiftstagrime.termuxrunner.ui.features.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.ui.components.ScriptPickerDialog
import io.github.swiftstagrime.termuxrunner.ui.theme.ScriptRunnerForTermuxTheme
import kotlinx.coroutines.launch
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition


@AndroidEntryPoint
class WidgetConfigurationActivity : ComponentActivity() {
    private val viewModel: WidgetConfigurationViewModel by viewModels()
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    private val selectedScriptIds = mutableStateListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_CONFIGURE ||
            intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
            appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@WidgetConfigurationActivity).getGlanceIdBy(appWidgetId)
            val prefs = getAppWidgetState(
                context = this@WidgetConfigurationActivity,
                definition = PreferencesGlanceStateDefinition,
                glanceId = glanceId
            )
            val existing = prefs[ScriptWidget.ScriptsListKey]?.split(",")?.filter { it.isNotEmpty() }?.mapNotNull { it.toIntOrNull() } ?: emptyList()
            selectedScriptIds.addAll(existing)
        }
        enableEdgeToEdge()
        setContent {
            val accent by viewModel.selectedAccent.collectAsStateWithLifecycle()
            val mode by viewModel.selectedMode.collectAsStateWithLifecycle()
            val allScripts by viewModel.allScripts.collectAsStateWithLifecycle()
            val categories by viewModel.allCategories.collectAsStateWithLifecycle()

            var showPicker by remember { mutableStateOf(false) }

            ScriptRunnerForTermuxTheme(accent = accent, mode = mode) {
                Scaffold(
                    topBar = {
                        Text("Widget Configuration", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.headlineSmall)
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = { saveAndExit() }) {
                            Icon(Icons.Default.Check, contentDescription = "Done")
                        }
                    }
                ) { padding ->
                    Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                        Text("Selected Scripts (${selectedScriptIds.size}/5)", modifier = Modifier.padding(16.dp))

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(selectedScriptIds) { id ->
                                val script = allScripts.find { it.id == id }
                                ListItem(
                                    headlineContent = { Text(script?.name ?: "Unknown Script") },
                                    trailingContent = {
                                        IconButton(onClick = { selectedScriptIds.remove(id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove")
                                        }
                                    }
                                )
                            }
                            if (selectedScriptIds.size < 5) {
                                item {
                                    OutlinedButton(
                                        onClick = { showPicker = true },
                                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                                    ) {
                                        Text("Add Script")
                                    }
                                }
                            }
                        }
                    }

                    if (showPicker) {
                        ScriptPickerDialog(
                            scripts = allScripts,
                            categories = categories,
                            onDismiss = { showPicker = false },
                            onScriptSelected = { script ->
                                if (!selectedScriptIds.contains(script.id)) {
                                    selectedScriptIds.add(script.id)
                                }
                                showPicker = false
                            }
                        )
                    }
                }
            }
        }
    }

    private fun saveAndExit() {
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@WidgetConfigurationActivity).getGlanceIdBy(appWidgetId)
            updateAppWidgetState(this@WidgetConfigurationActivity, glanceId) { prefs ->
                prefs[ScriptWidget.ScriptsListKey] = selectedScriptIds.joinToString(",")
            }
            ScriptWidget().update(this@WidgetConfigurationActivity, glanceId)

            val resultValue = Intent().apply { putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId) }
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}