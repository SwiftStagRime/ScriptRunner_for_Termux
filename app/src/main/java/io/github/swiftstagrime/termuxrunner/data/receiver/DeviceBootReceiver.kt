package io.github.swiftstagrime.termuxrunner.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.swiftstagrime.termuxrunner.data.automation.AutomationScheduler
import io.github.swiftstagrime.termuxrunner.data.automation.recordDispatchFailure
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationLogDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.ScriptDao
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.data.service.WebhookService
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.RunScriptUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DeviceBootReceiver : BroadcastReceiver() {
    @Inject
    lateinit var automationDao: AutomationDao

    @Inject
    lateinit var automationLogDao: AutomationLogDao

    @Inject
    lateinit var scheduler: AutomationScheduler

    @Inject
    lateinit var runScriptUseCase: RunScriptUseCase

    @Inject
    lateinit var scriptDao: ScriptDao

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    /**
     * Set in [onReceive]; [handleBoot] uses it as the default so tests can call
     * [handleBoot] directly without a receiver context.
     */
    private var receiverContext: Context? = null

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            receiverContext = context.applicationContext
            val pendingResult = goAsync()
            handleBoot(pendingResult)
        }
    }

    internal fun handleBoot(
        pendingResult: PendingResult? = null,
        context: Context? = receiverContext,
    ) {
        if (context == null) return
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            try {
                scheduler.ensureSweepEnqueued()
                scheduler.reconcileEventWatcher()

                val allAutomations = automationDao.getEnabledAutomations()
                allAutomations.forEach { automation ->
                    if (automation.type == AutomationType.BOOT) {
                        runBootAutomation(automation, context)
                    } else {
                        scheduler.schedule(automation)
                    }
                }

                if (userPreferencesRepository.isWebhookEnabled.first()) {
                    Log.d("DeviceBootReceiver", "Auto-starting WebhookService")
                    ContextCompat.startForegroundService(
                        context.applicationContext,
                        Intent(context.applicationContext, WebhookService::class.java),
                    )
                }
            } catch (e: Exception) {
                Log.e("DeviceBootReceiver", "Error handling boot automations", e)
            } finally {
                scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
                pendingResult?.finish()
            }
        }
    }

    /**
     * Runs one BOOT automation. The run itself is recorded by Termux's result
     * broadcast, never here: handing the command to the system does not prove it
     * arrived, and a boot into a force-stopped Termux used to show up as a
     * completed run. A failing dispatch is logged for this automation only, so
     * one broken script cannot stop the remaining boot work.
     */
    private suspend fun runBootAutomation(
        automation: AutomationEntity,
        context: Context,
    ) {
        val scriptEntity = scriptDao.getScriptById(automation.scriptId)

        val dispatchError =
            if (scriptEntity != null) {
                try {
                    runScriptUseCase(
                        script = scriptEntity.toScriptDomain(),
                        runtimeArgs = automation.runtimeArgs,
                        runtimeEnv = automation.runtimeEnv,
                        runtimePrefix = automation.runtimePrefix,
                        automationId = automation.id,
                    )
                    null
                } catch (e: Exception) {
                    e
                }
            } else {
                null
            }

        if (dispatchError == null) return

        Log.e(
            "DeviceBootReceiver",
            "Boot automation ${automation.id} (${automation.label}) failed to dispatch",
            dispatchError,
        )
        automationLogDao.recordDispatchFailure(
            automationId = automation.id,
            error = dispatchError,
            context = context,
            tag = "DeviceBootReceiver",
        )
    }
}
