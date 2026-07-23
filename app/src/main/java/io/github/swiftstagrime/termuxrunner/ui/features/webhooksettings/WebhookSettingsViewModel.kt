package io.github.swiftstagrime.termuxrunner.ui.features.webhooksettings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.swiftstagrime.termuxrunner.data.service.WebhookConfig
import io.github.swiftstagrime.termuxrunner.data.service.WebhookServiceState
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class WebhookSettingsState(
    val isEnabled: Boolean = false,
    val isRunning: Boolean = false,
    val port: Int = 0,
    val token: String? = null,
    val lanAccess: Boolean = false,
)

data class WebhookSettingsActions(
    val onBack: () -> Unit,
    val onToggleEnabled: (Boolean) -> Unit,
    val onToggleLanAccess: (Boolean) -> Unit,
    val onRegenerateToken: () -> Unit,
    val onPortChange: (Int) -> Unit,
)

@HiltViewModel
class WebhookSettingsViewModel
    @Inject
    constructor(
        private val userPreferencesRepository: UserPreferencesRepository,
        webhookServiceState: WebhookServiceState,
        val webhookConfig: WebhookConfig,
    ) : ViewModel() {
        val isWebhookEnabled: Flow<Boolean> = userPreferencesRepository.isWebhookEnabled

        val isServiceRunning: Flow<Boolean> = webhookServiceState.isRunning

        suspend fun setWebhookEnabled(enabled: Boolean) {
            userPreferencesRepository.setWebhookEnabled(enabled)
        }
    }
