package io.github.swiftstagrime.termuxrunner.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationDao
import io.github.swiftstagrime.termuxrunner.data.worker.AutomationWorker
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType

class EventReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "EventReceiver"
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action.isNullOrEmpty()) return

        val eventAutomationType = mapActionToType(context, intent) ?: return

        enqueueWorkerForType(context, eventAutomationType)
    }

    private fun mapActionToType(
        context: Context,
        intent: Intent,
    ): AutomationType? =
        when (intent.action) {
            Intent.ACTION_SCREEN_ON, "android.intent.action.USER_PRESENT" -> AutomationType.SCREEN_ON
            "android.intent.action.SCREEN_OFF" -> AutomationType.SCREEN_OFF

            ConnectivityManager.CONNECTIVITY_ACTION -> {
                val cm =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
                val isConnected =
                    capabilities != null &&
                        (
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        )
                if (isConnected) AutomationType.NETWORK_CONNECTED else AutomationType.NETWORK_DISCONNECTED
            }

            "android.hardware.usb.action.USB_DEVICE_ATTACHED" -> AutomationType.USB_CONNECTED
            "android.hardware.usb.action.USB_DEVICE_DETACHED" -> AutomationType.USB_DISCONNECTED

            else -> null
        }

    private fun enqueueWorkerForType(
        context: Context,
        automationType: AutomationType,
    ) {
        val workRequest =
            OneTimeWorkRequestBuilder<EventAutomationWorker>()
                .setInputData(workDataOf("event_type" to automationType.name))
                .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "event_${automationType.name}_${System.currentTimeMillis()}",
            ExistingWorkPolicy.APPEND,
            workRequest,
        )
    }
}

@HiltWorker
class EventAutomationWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        private val automationDao: AutomationDao,
    ) : CoroutineWorker(context, workerParams) {
        override suspend fun doWork(): Result {
            val eventTypeName = inputData.getString("event_type") ?: return Result.failure()
            val eventType =
                runCatching { AutomationType.valueOf(eventTypeName) }.getOrNull()
                    ?: return Result.failure()

            val enabledAutomations =
                automationDao.getEnabledAutomations().filter { it.type == eventType }

            for (automation in enabledAutomations) {
                val workRequest =
                    androidx.work
                        .OneTimeWorkRequestBuilder<AutomationWorker>()
                        .setInputData(workDataOf("automation_id" to automation.id))
                        .build()
                WorkManager.getInstance(applicationContext).enqueue(workRequest)
            }

            return Result.success()
        }
    }
