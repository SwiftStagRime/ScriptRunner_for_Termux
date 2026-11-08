package io.github.swiftstagrime.termuxrunner.data.automation

import android.content.Context
import android.util.Log
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationLogDao
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationLogEntity
import io.github.swiftstagrime.termuxrunner.data.repository.TermuxException
import io.github.swiftstagrime.termuxrunner.data.worker.AutomationWorker

/**
 * Records in the automation log that a run never reached Termux. Shared by every
 * dispatch path (scheduled worker, boot receiver) so the user always sees the same
 * explanation for the same failure.
 *
 * A failure to write the log is never fatal: the dispatch already failed, and a
 * database problem must not replace the original error.
 */
internal suspend fun AutomationLogDao.recordDispatchFailure(
    automationId: Int,
    error: Throwable,
    context: Context,
    tag: String,
) {
    runCatching {
        // Termux-specific failures carry a localized explanation; anything else
        // only has its exception message, and a bare class name helps nobody.
        val reason =
            when (error) {
                is TermuxException -> error.uiText.asString(context)
                else -> error.message ?: error::class.simpleName ?: "unknown error"
            }
        insertLog(
            AutomationLogEntity(
                automationId = automationId,
                timestamp = System.currentTimeMillis(),
                exitCode = AutomationWorker.DISPATCH_FAILED_EXIT_CODE,
                message = "Could not start script: $reason",
            ),
        )
    }.onFailure { logError ->
        Log.w(tag, "Could not persist failure log for automation $automationId", logError)
    }
}
