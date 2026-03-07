package io.github.swiftstagrime.termuxrunner.ui.features.widget.script

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dagger.hilt.android.EntryPointAccessors
import io.github.swiftstagrime.termuxrunner.di.WidgetEntryPoint
import io.github.swiftstagrime.termuxrunner.domain.model.InteractionMode
import io.github.swiftstagrime.termuxrunner.ui.features.runner.ScriptRunnerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RunScriptAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val scriptId = parameters[ScriptWidget.ScriptIdActionKey] ?: return
        if (scriptId == -1) {
            return
        }

        val entryPoint =
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetEntryPoint::class.java,
            )

        val scriptRepository = entryPoint.scriptRepository()
        val runScriptUseCase = entryPoint.runScriptUseCase()
        val script = scriptRepository.getScriptById(scriptId) ?: return

        val requiresInput = script.interactionMode != InteractionMode.NONE
        val opensWindow = script.openNewSession

        if (requiresInput || opensWindow) {
            val intent =
                Intent(context, ScriptRunnerActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("SCRIPT_ID", script.id)
                }
            context.startActivity(intent)
        } else {
            try {
                runScriptUseCase(script)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
