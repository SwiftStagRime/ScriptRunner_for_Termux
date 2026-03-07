package io.github.swiftstagrime.termuxrunner.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import io.github.swiftstagrime.termuxrunner.data.automation.AutomationScheduler
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.ScriptDao
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import io.github.swiftstagrime.termuxrunner.domain.usecase.RunScriptUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DeviceBootReceiver : BroadcastReceiver() {
    @Inject
    lateinit var automationDao: AutomationDao

    @Inject
    lateinit var scheduler: AutomationScheduler

    @Inject
    lateinit var runScriptUseCase: RunScriptUseCase

    @Inject
    lateinit var scriptDao: ScriptDao

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                try {
                    val allAutomations = automationDao.getEnabledAutomations()

                    allAutomations.forEach { automation ->
                        when (automation.type) {
                            AutomationType.BOOT -> {
                                val scriptEntity = scriptDao.getScriptById(automation.scriptId)
                                if (scriptEntity != null) {
                                    runScriptUseCase(
                                        script = scriptEntity.toScriptDomain(),
                                        runtimeArgs = automation.runtimeArgs,
                                        runtimeEnv = automation.runtimeEnv,
                                        runtimePrefix = automation.runtimePrefix,
                                        automationId = automation.id,
                                    )
                                }

                                automationDao.updateAutomation(
                                    automation.copy(
                                        lastRunTimestamp = System.currentTimeMillis(),
                                    )
                                )
                            }

                            else -> {
                                scheduler.schedule(automation)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
