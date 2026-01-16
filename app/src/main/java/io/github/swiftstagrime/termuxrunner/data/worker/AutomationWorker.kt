package io.github.swiftstagrime.termuxrunner.data.worker

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.swiftstagrime.termuxrunner.data.automation.AutomationScheduler
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.ScriptDao
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.domain.usecase.RunScriptUseCase
import io.github.swiftstagrime.termuxrunner.domain.util.AutomationTimeCalculator

@HiltWorker
class AutomationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val automationDao: AutomationDao,
    private val scriptDao: ScriptDao,
    private val runScriptUseCase: RunScriptUseCase,
    private val scheduler: AutomationScheduler
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val id = inputData.getInt("automation_id", -1)
        val automation = automationDao.getAutomationById(id) ?: return Result.failure()

        if (!checkConditions(automation)) {
            return Result.success()
        }

        val nextRun = AutomationTimeCalculator.calculateNextRun(automation)

        val updatedAutomation = automation.copy(
            lastRunTimestamp = System.currentTimeMillis(),
            nextRunTimestamp = nextRun,
            isEnabled = (nextRun != null)
        )
        automationDao.updateAutomation(updatedAutomation)

        val scriptEntity = scriptDao.getScriptById(automation.scriptId)
        if (scriptEntity != null) {
            runScriptUseCase(
                script = scriptEntity.toScriptDomain(),
                runtimeArgs = automation.runtimeArgs,
                runtimeEnv = automation.runtimeEnv,
                runtimePrefix = automation.runtimePrefix,
                automationId = automation.id
            )
        }

        if (updatedAutomation.isEnabled && nextRun != null) {
            scheduler.schedule(updatedAutomation)
        }

        return Result.success()
    }

    private fun checkConditions(automation: AutomationEntity): Boolean {

        if (automation.requireWifi) {
            val cm =
                applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
            if (capabilities == null || !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return false
        }

        val bm = applicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        if (automation.requireCharging) {
            val isCharging = bm.isCharging
            if (!isCharging) {
                val intent = applicationContext.registerReceiver(
                    null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                )
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val charging =
                    status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                if (!charging) return false
            }
        }

        if (automation.batteryThreshold > 0) {
            val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val finalLevel = if (level <= 0) {
                val intent = applicationContext.registerReceiver(
                    null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                )
                intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            } else level

            if (finalLevel != -1 && finalLevel < automation.batteryThreshold) return false
        }

        return true
    }
}