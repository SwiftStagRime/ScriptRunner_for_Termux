package io.github.swiftstagrime.termuxrunner.ui.features.automation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.domain.model.Automation
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import io.github.swiftstagrime.termuxrunner.domain.model.Category
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.model.ScriptRuntimeParams
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

        fun saveAutomation(
            scriptId: Int,
            label: String,
            type: AutomationType,
            timestamp: Long,
            interval: Long = 0,
            days: List<Int> = emptyList(),
            requireWifi: Boolean = false,
            requireCharging: Boolean = false,
            runIfMissed: Boolean = true,
            batteryThreshold: Int = 0,
            runtime: ScriptRuntimeParams,
        ) {
            viewModelScope.launch {
                val tempEntity =
                    AutomationEntity(
                        id = 0,
                        scriptId = scriptId,
                        label = label,
                        type = type,
                        scheduledTimestamp = timestamp,
                        intervalMillis = interval,
                        daysOfWeek = days,
                        isEnabled = true,
                        runIfMissed = runIfMissed,
                        requireWifi = requireWifi,
                        requireCharging = requireCharging,
                        batteryThreshold = batteryThreshold,
                    )

                val nextRun =
                    AutomationTimeCalculator.calculateNextRun(
                        automation = tempEntity,
                        fromTime = System.currentTimeMillis(),
                    )

                val automation =
                    Automation(
                        id = 0,
                        scriptId = scriptId,
                        label = label,
                        type = type,
                        scheduledTimestamp = timestamp,
                        intervalMillis = interval,
                        daysOfWeek = days,
                        isEnabled = true,
                        runIfMissed = runIfMissed,
                        nextRunTimestamp = nextRun,
                        runtimeArgs = runtime.arguments,
                        runtimePrefix = runtime.prefix,
                        runtimeEnv = runtime.envVars,
                        requireWifi = requireWifi,
                        requireCharging = requireCharging,
                        batteryThreshold = batteryThreshold,
                        lastRunTimestamp = null,
                        lastExitCode = null,
                    )

                automationRepository.saveAutomation(automation)
            }
        }
    }
