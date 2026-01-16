package io.github.swiftstagrime.termuxrunner.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.AndroidEntryPoint
import io.github.swiftstagrime.termuxrunner.data.worker.AutomationWorker

@AndroidEntryPoint
class AutomationReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val automationId = intent.getIntExtra("automation_id", -1)
        if (automationId == -1) return

        val workRequest =
            OneTimeWorkRequestBuilder<AutomationWorker>()
                .setInputData(workDataOf("automation_id" to automationId))
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
