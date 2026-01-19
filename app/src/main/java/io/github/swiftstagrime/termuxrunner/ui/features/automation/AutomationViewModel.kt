package io.github.swiftstagrime.termuxrunner.ui.features.automation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.domain.model.Automation
import io.github.swiftstagrime.termuxrunner.domain.model.Category
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationLogRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.CategoryRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.RunScriptUseCase
import io.github.swiftstagrime.termuxrunner.domain.util.AutomationTimeCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AutomationUiState(
    val items: List<AutomationUiItem> = emptyList(),
    val isLoading: Boolean = false,
    val isExactAlarmPermissionGranted: Boolean = true,
)

@HiltViewModel
class AutomationViewModel
    @Inject
    constructor(
        private val automationRepository: AutomationRepository,
        private val scriptRepository: ScriptRepository,
        categoryRepository: CategoryRepository,
        private val automationLogRepository: AutomationLogRepository,
        private val runScriptUseCase: RunScriptUseCase,
    ) : ViewModel() {
        val automations: StateFlow<List<Automation>> =
            automationRepository
                .getAllAutomations()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val allScripts: StateFlow<List<Script>> =
            scriptRepository
                .getAllScripts()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val allCategories: StateFlow<List<Category>> =
            categoryRepository
                .getAllCategories()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        fun toggleAutomation(
            id: Int,
            enabled: Boolean,
        ) {
            viewModelScope.launch { automationRepository.toggleAutomation(id, enabled) }
        }

        fun deleteAutomation(automation: Automation) {
            viewModelScope.launch { automationRepository.deleteAutomation(automation) }
        }

        fun runAutomationNow(automation: Automation) {
            viewModelScope.launch {
                scriptRepository.getScriptById(automation.scriptId)?.let { script ->
                    runScriptUseCase(
                        script = script,
                        runtimeArgs = automation.runtimeArgs,
                        runtimeEnv = automation.runtimeEnv,
                        runtimePrefix = automation.runtimePrefix,
                        automationId = automation.id,
                    )
                }
            }
        }

        fun getAutomationLogs(automationId: Int) = automationLogRepository.getLogsForAutomation(automationId)

        fun saveAutomation(params: AutomationSaveParams) {
            viewModelScope.launch {
                val now = System.currentTimeMillis()

                val tempEntity = AutomationEntity(
                    id = 0,
                    scriptId = params.scriptId,
                    label = params.label,
                    type = params.type,
                    scheduledTimestamp = params.timestamp,
                    intervalMillis = params.interval,
                    daysOfWeek = params.days,
                    isEnabled = true,
                    runIfMissed = params.runIfMissed,
                    requireWifi = params.requireWifi,
                    requireCharging = params.requireCharging,
                    batteryThreshold = params.batteryThreshold,
                )

                val nextRun = AutomationTimeCalculator.calculateNextRun(
                    automation = tempEntity,
                    fromTime = now,
                )

                val automation = Automation(
                    id = 0,
                    scriptId = params.scriptId,
                    label = params.label,
                    type = params.type,
                    scheduledTimestamp = params.timestamp,
                    intervalMillis = params.interval,
                    daysOfWeek = params.days,
                    isEnabled = true,
                    runIfMissed = params.runIfMissed,
                    nextRunTimestamp = nextRun,
                    runtimeArgs = params.runtime.arguments,
                    runtimePrefix = params.runtime.prefix,
                    runtimeEnv = params.runtime.envVars,
                    requireWifi = params.requireWifi,
                    requireCharging = params.requireCharging,
                    batteryThreshold = params.batteryThreshold,
                    lastRunTimestamp = null,
                    lastExitCode = null,
                )

                automationRepository.saveAutomation(automation)
            }
        }
    }