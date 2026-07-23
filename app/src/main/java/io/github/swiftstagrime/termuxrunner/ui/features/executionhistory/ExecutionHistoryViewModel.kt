package io.github.swiftstagrime.termuxrunner.ui.features.executionhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.swiftstagrime.termuxrunner.di.IoDispatcher
import io.github.swiftstagrime.termuxrunner.domain.model.ScriptExecution
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptExecutionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExecutionHistoryUiState(
    val executions: List<ScriptExecution> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ExecutionHistoryViewModel
    @Inject
    constructor(
        private val scriptExecutionRepository: ScriptExecutionRepository,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val scriptIdFilter = MutableStateFlow<Int?>(null)

        fun setScriptIdFilter(scriptId: Int?) {
            scriptIdFilter.value = scriptId
        }

        val uiState =
            combine(
                scriptIdFilter,
                scriptExecutionRepository.getRecentExecutions(200),
            ) { scriptId, allExecutions ->
                if (scriptId != null) {
                    allExecutions.filter { it.scriptId == scriptId }
                } else {
                    allExecutions
                }
            }.map { executions ->
                ExecutionHistoryUiState(
                    executions = executions,
                    isLoading = false,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ExecutionHistoryUiState(isLoading = true),
            )

        fun clearAll() {
            viewModelScope.launch(ioDispatcher) {
                scriptExecutionRepository.clearAll()
            }
        }

        fun deleteOldRecords(days: Int = 30) {
            viewModelScope.launch(ioDispatcher) {
                val threshold = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000)
                scriptExecutionRepository.deleteOldRecords(threshold)
            }
        }

        fun deleteForScript(scriptId: Int) {
            viewModelScope.launch(ioDispatcher) {
                scriptExecutionRepository.deleteForScript(scriptId)
            }
        }
    }
