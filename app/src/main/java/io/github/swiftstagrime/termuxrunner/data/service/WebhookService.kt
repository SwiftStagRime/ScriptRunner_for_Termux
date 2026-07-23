package io.github.swiftstagrime.termuxrunner.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.AndroidEntryPoint
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.data.worker.AutomationWorker
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.RunScriptUseCase
import io.github.swiftstagrime.termuxrunner.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WebhookService : Service() {
    @Inject
    lateinit var scriptRepository: ScriptRepository

    @Inject
    lateinit var automationRepository: AutomationRepository

    @Inject
    lateinit var runScriptUseCase: RunScriptUseCase

    @Inject
    lateinit var webhookConfig: WebhookConfig

    @Inject
    lateinit var webhookServiceState: WebhookServiceState

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var httpServer: WebhookHttpServer? = null

    companion object {
        private const val NOTIFICATION_ID = 4201
        private const val CHANNEL_ID = "webhook_service"
        const val ACTION_STOP = "ACTION_STOP_WEBHOOK"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val port = webhookConfig.getPortOrGenerate()
        val notification = buildNotification(port)
        startForeground(NOTIFICATION_ID, notification)

        serviceScope.launch {
            val bindAddress = if (webhookConfig.lanAccess) "0.0.0.0" else "127.0.0.1"

            try {
                httpServer =
                    WebhookHttpServer(bindAddress, port).apply {
                        start(
                            NanoHTTPD.SOCKET_READ_TIMEOUT,
                            false,
                        )
                    }
                webhookServiceState.setRunning(true)
            } catch (e: Exception) {
                webhookServiceState.setRunning(false)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        httpServer?.stop()
        serviceScope.coroutineContext[Job]?.cancelChildren()
        webhookServiceState.setRunning(false)
    }

    private inner class WebhookHttpServer(
        bindAddress: String,
        port: Int,
    ) : NanoHTTPD(bindAddress, port) {
        override fun serve(session: IHTTPSession): Response {
            if (session.method != Method.POST && session.method != Method.GET) {
                return newFixedLengthResponse(
                    Response.Status.METHOD_NOT_ALLOWED,
                    "application/json",
                    "{\"error\": \"Method not allowed\"}",
                )
            }

            val uri = session.uri
            return when {
                uri.startsWith("/trigger/automation/") -> handleAutomationTrigger(uri, session)
                uri.startsWith("/trigger/script/") -> handleScriptTrigger(uri, session)
                else ->
                    newFixedLengthResponse(
                        Response.Status.NOT_FOUND,
                        "application/json",
                        "{\"error\": \"Not found\"}",
                    )
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Webhook Server",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Local webhook server for triggering scripts and automations"
                }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(port: Int): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val stopIntent =
            Intent(this, WebhookService::class.java).apply {
                action = ACTION_STOP
            }
        val stopPendingIntent =
            PendingIntent.getService(
                this,
                1,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setContentTitle("Webhook Server Active")
            .setContentText("Port: $port")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.webhook_stop_server),
                stopPendingIntent,
            ).setOngoing(true)
            .build()
    }

    private fun handleAutomationTrigger(
        uri: String,
        session: IHTTPSession,
    ): NanoHTTPD.Response {
        if (!authenticateRequest(session)) {
            return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.UNAUTHORIZED,
                "application/json",
                "{\"error\": \"Unauthorized\"}",
            )
        }

        val code = uri.removePrefix("/trigger/automation/")
        if (code.isBlank()) {
            return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.BAD_REQUEST,
                "application/json",
                "{\"error\": \"Missing automation code\"}",
            )
        }

        val automationId = code.toIntOrNull()
        serviceScope.launch {
            val automation =
                if (automationId != null) {
                    automationRepository.getAutomationById(automationId)
                        ?: automationRepository.getAutomationByAdbCode(code).getOrNull()
                } else {
                    automationRepository.getAutomationByAdbCode(code).getOrNull()
                }

            if (automation != null) {
                triggerAutomationViaWorkManager(automation.id)
            } else {
                Log.e("WebhookService", "Automation not found for code: $code")
            }
        }
        return NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK,
            "application/json",
            "{\"status\": \"triggered\", \"automation_code\": \"$code\"}",
        )
    }

    private fun handleScriptTrigger(
        uri: String,
        session: IHTTPSession,
    ): NanoHTTPD.Response {
        if (!authenticateRequest(session)) {
            return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.UNAUTHORIZED,
                "application/json",
                "{\"error\": \"Unauthorized\"}",
            )
        }

        val code = uri.removePrefix("/trigger/script/")
        if (code.isBlank()) {
            return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.BAD_REQUEST,
                "application/json",
                "{\"error\": \"Missing script code\"}",
            )
        }

        serviceScope.launch {
            val script =
                runCatching {
                    val numericId = code.toIntOrNull()
                    if (numericId != null) {
                        scriptRepository.getScriptById(numericId)
                            ?: scriptRepository.getScriptByAdbCode(code).getOrNull()
                    } else {
                        scriptRepository.getScriptByAdbCode(code).getOrNull()
                    }
                }.getOrNull()

            if (script != null) {
                runScriptUseCase(
                    script = script.copy(notifyOnResult = true),
                )
            } else {
                Log.e("WebhookService", "Script not found for code: $code")
            }
        }
        return NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.ACCEPTED,
            "application/json",
            "{\"status\": \"accepted\", \"script_code\": \"$code\"}",
        )
    }

    private fun triggerAutomationViaWorkManager(automationId: Int) {
        val workRequest =
            OneTimeWorkRequestBuilder<AutomationWorker>()
                .setInputData(workDataOf("automation_id" to automationId))
                .build()
        WorkManager
            .getInstance(this)
            .enqueue(workRequest)
    }

    private fun authenticateRequest(session: IHTTPSession): Boolean {
        val token = webhookConfig.token
        if (token.isNullOrBlank()) return true

        val authHeader = session.headers["authorization"]
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val headerToken = authHeader.removePrefix("Bearer ").trim()
            if (headerToken == token) return true
        }

        val queryToken = session.parms["token"]
        return !queryToken.isNullOrBlank() && queryToken == token
    }
}
