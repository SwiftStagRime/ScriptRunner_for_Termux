package io.github.swiftstagrime.termuxrunner.ui.features.webhooksettings

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.swiftstagrime.termuxrunner.data.service.WebhookService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun WebhookSettingsRoute(
    onBack: () -> Unit,
    viewModel: WebhookSettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val webhookConfig = viewModel.webhookConfig

    LaunchedEffect(webhookConfig) {
        webhookConfig.getPortOrGenerate()
    }

    var refreshTrigger by remember { mutableIntStateOf(0) }

    fun invalidateAll() {
        refreshTrigger++
    }

    val isEnabled by viewModel.isWebhookEnabled.collectAsStateWithLifecycle(initialValue = false)
    val isRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle(initialValue = false)

    val webhookValues by produceState(
        initialValue = Triple(8080, "" as String?, false),
        key1 = refreshTrigger,
        key2 = isEnabled,
    ) {
        value =
            Triple(
                webhookConfig.port.takeIf { it > 0 } ?: 8080,
                webhookConfig.token,
                webhookConfig.lanAccess,
            )
    }

    val port = webhookValues.first
    val token = webhookValues.second
    val lanAccess = webhookValues.third

    val state =
        remember(isEnabled, isRunning, port, token, lanAccess) {
            WebhookSettingsState(
                isEnabled = isEnabled,
                isRunning = isRunning,
                port = port,
                token = token,
                lanAccess = lanAccess,
            )
        }

    LaunchedEffect(isEnabled, isRunning) {
        if (isEnabled && !isRunning) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, WebhookService::class.java),
            )
        } else if (!isEnabled && isRunning) {
            context.stopService(Intent(context, WebhookService::class.java))
        }
    }

    fun restartWebhook() {
        context.stopService(Intent(context, WebhookService::class.java))
        scope.launch {
            delay(500.milliseconds)
            ContextCompat.startForegroundService(
                context,
                Intent(context, WebhookService::class.java),
            )
        }
    }

    val actions =
        rememberUpdatedState(
            WebhookSettingsActions(
                onBack = onBack,
                onToggleEnabled = { enabled ->
                    scope.launch {
                        viewModel.setWebhookEnabled(enabled)
                    }
                },
                onToggleLanAccess = { enabled ->
                    webhookConfig.lanAccess = enabled
                    invalidateAll()
                    if (isRunning && isEnabled) {
                        restartWebhook()
                    }
                },
                onRegenerateToken = {
                    webhookConfig.generateToken()
                    invalidateAll()
                },
                onPortChange = { newPort ->
                    if (newPort in 1024..65535) {
                        webhookConfig.port = newPort
                        invalidateAll()
                        if (isRunning && isEnabled) {
                            restartWebhook()
                        }
                    }
                },
            ),
        )

    WebhookSettingsScreen(state, actions.value)
}
