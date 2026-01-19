package io.github.swiftstagrime.termuxrunner.data.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.model.InteractionMode
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.RunScriptUseCase
import io.github.swiftstagrime.termuxrunner.ui.features.runner.ScriptRunnerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
abstract class BaseScriptTileService : TileService() {

    abstract val tileIndex: Int

    @Inject
    lateinit var scriptRepository: ScriptRepository

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var runScriptUseCase: RunScriptUseCase

    // We use a supervisor scope to ensure background work isn't cancelled immediately
    // if the UI disconnects, though TileService lifecycle is short.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var assignedScriptId: Int? = null

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        val scriptId = assignedScriptId

        if (scriptId == null) {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            launchIntent?.let { safeStartActivityAndCollapse(it) }
            return
        }

        serviceScope.launch {
            val script = scriptRepository.getScriptById(scriptId) ?: return@launch

            val requiresInput = script.interactionMode != InteractionMode.NONE
            val opensWindow = script.openNewSession

            if (requiresInput || opensWindow) {
                val intent = Intent(this@BaseScriptTileService, ScriptRunnerActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("SCRIPT_ID", script.id)
                }
                safeStartActivityAndCollapse(intent)
            } else {
                try {
                    qsTile?.let { tile ->
                        tile.state = Tile.STATE_UNAVAILABLE
                        tile.updateTile()

                        runScriptUseCase(script)

                        Toast.makeText(
                            applicationContext,
                            getString(R.string.msg_script_executed, script.name),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        applicationContext,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    qsTile?.let { tile ->
                        tile.state = Tile.STATE_ACTIVE
                        tile.updateTile()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun updateTileState() {
        serviceScope.launch {
            val scriptId = userPreferencesRepository.getScriptIdForTile(tileIndex).firstOrNull()
            assignedScriptId = scriptId

            val tile = qsTile ?: return@launch

            if (scriptId == null) {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.tile_unassigned)
                tile.updateTile()
                return@launch
            }

            val script = scriptRepository.getScriptById(scriptId)
            if (script == null) {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.tile_deleted_script)
            } else {
                tile.state = Tile.STATE_ACTIVE
                tile.label = script.name
            }
            tile.updateTile()
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun safeStartActivityAndCollapse(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
