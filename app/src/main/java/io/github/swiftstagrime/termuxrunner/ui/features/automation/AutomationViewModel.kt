package io.github.swiftstagrime.termuxrunner.ui.features.automation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.di.IoDispatcher
import io.github.swiftstagrime.termuxrunner.domain.model.Automation
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationChain
import io.github.swiftstagrime.termuxrunner.domain.model.Category
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationChainRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationLogRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.CategoryRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.RunScriptUseCase
import io.github.swiftstagrime.termuxrunner.domain.util.AutomationTimeCalculator
import io.github.swiftstagrime.termuxrunner.ui.utils.WidgetManager
import kotlinx.coroutines.CoroutineDispatcher
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
        private val chainRepository: AutomationChainRepository,
        private val runScriptUseCase: RunScriptUseCase,
        private val widgetManager: WidgetManager,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
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
            viewModelScope.launch(ioDispatcher) {
                automationRepository.toggleAutomation(id, enabled)
                widgetManager.updateAutomationWidget()
            }
        }

        fun deleteAutomation(automation: Automation) {
            viewModelScope.launch(ioDispatcher) {
                automationRepository.deleteAutomation(automation)
                widgetManager.updateAutomationWidget()
            }
        }

        fun runAutomationNow(automation: Automation) {
            viewModelScope.launch(ioDispatcher) {
                scriptRepository.getScriptById(automation.scriptId)?.let { script ->
                    runScriptUseCase(
                        script = script.copy(notifyOnResult = true),
                        runtimeArgs = automation.runtimeArgs,
                        runtimeEnv = automation.runtimeEnv,
                        runtimePrefix = automation.runtimePrefix,
                        automationId = automation.id,
                    )
                }
                widgetManager.updateLogsWidget()
            }
        }

        fun saveAutomation(params: AutomationSaveParams) {
            viewModelScope.launch(ioDispatcher) {
                val now = System.currentTimeMillis()

                val tempEntity =
                    AutomationEntity(
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
                        scheduledDayOfMonth = params.scheduledDayOfMonth,
                        windowStartHour = params.windowStartHour,
                        windowStartMinute = params.windowStartMinute,
                        windowEndHour = params.windowEndHour,
                        windowEndMinute = params.windowEndMinute,
                        randomDelayMinMillis = params.randomDelayMinMillis,
                        randomDelayMaxMillis = params.randomDelayMaxMillis,
                        automationCode = params.automationCode.ifBlank { null },
                    )

                val nextRun =
                    AutomationTimeCalculator.calculateNextRun(
                        automation = tempEntity,
                        fromTime = now,
                    )

                val automation =
                    Automation(
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
                        scheduledDayOfMonth = params.scheduledDayOfMonth,
                        windowStartHour = params.windowStartHour,
                        windowStartMinute = params.windowStartMinute,
                        windowEndHour = params.windowEndHour,
                        windowEndMinute = params.windowEndMinute,
                        randomDelayMinMillis = params.randomDelayMinMillis,
                        randomDelayMaxMillis = params.randomDelayMaxMillis,
                        automationCode = params.automationCode.ifBlank { null },
                    )

                automationRepository.saveAutomation(automation)
                widgetManager.updateAutomationWidget()
            }
        }

        fun getAutomationLogs(automationId: Int) = automationLogRepository.getLogsForAutomation(automationId)

        suspend fun saveChain(chain: AutomationChain) {
            require(chain.name.isNotBlank()) { "Chain name cannot be blank" }
            require(chain.steps.isNotEmpty()) { "Chain must have at least one step" }
            chainRepository.saveChain(chain)
        }

        suspend fun updateChain(chain: AutomationChain) {
            require(chain.name.isNotBlank()) { "Chain name cannot be blank" }
            require(chain.steps.isNotEmpty()) { "Chain must have at least one step" }
            chainRepository.updateChain(chain)
        }

        suspend fun getChainsByTriggerId(automationId: Int): List<AutomationChain> = chainRepository.getChainsByTriggerId(automationId)
    }
