package io.github.swiftstagrime.termuxrunner.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.swiftstagrime.termuxrunner.data.service.HeartbeatService
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.MonitoringRepository
import javax.inject.Inject

/**
 * Repository for managing monitoring-related operations.
 */

class MonitoringRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : MonitoringRepository {
        override fun hasNotificationPermission(): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

        override fun startMonitoring(script: Script) {
            if (!hasNotificationPermission()) return

            val intent =
                Intent(context, HeartbeatService::class.java).apply {
                    action = HeartbeatService.ACTION_START
                    putExtra(HeartbeatService.EXTRA_SCRIPT_ID, script.id)
                    putExtra(HeartbeatService.EXTRA_SCRIPT_NAME, script.name)
                    putExtra(HeartbeatService.EXTRA_TIMEOUT_MS, script.heartbeatTimeout)
                }

            ContextCompat.startForegroundService(context, intent)
        }

        override fun stopMonitoring() {
            val intent =
                Intent(context, HeartbeatService::class.java).apply {
                    action = HeartbeatService.ACTION_STOP
                }
            context.stopService(intent)
        }
    }
