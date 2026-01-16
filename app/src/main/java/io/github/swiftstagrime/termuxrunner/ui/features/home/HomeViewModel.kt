package io.github.swiftstagrime.termuxrunner.ui.features.home

import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.data.repository.TermuxException
import io.github.swiftstagrime.termuxrunner.data.repository.TermuxPermissionException
import io.github.swiftstagrime.termuxrunner.domain.model.Category
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.CategoryRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.IconRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ShortcutRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.DeleteScriptUseCase
import io.github.swiftstagrime.termuxrunner.domain.usecase.RunScriptUseCase
import io.github.swiftstagrime.termuxrunner.domain.usecase.UpdateScriptUseCase
import io.github.swiftstagrime.termuxrunner.ui.extensions.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(
        val scripts: List<Script>,
        val categories: List<Category>,
        val tileMappings: Map<Int, Script?>,
    ) : HomeUiState
}

sealed interface HomeUiEvent {
    data class ShowSnackbar(
        val message: UiText,
    ) : HomeUiEvent

    data object RequestTermuxPermission : HomeUiEvent

    data class CreateShortcut(
        val shortcutInfo: ShortcutInfoCompat,
    ) : HomeUiEvent
}

enum class SortOption {
    NAME_ASC,
    NAME_DESC,
    DATE_NEWEST,
    DATE_OLDEST,
    MANUAL,
}

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val runScriptUseCase: RunScriptUseCase,
        private val deleteScriptUseCase: DeleteScriptUseCase,
        private val updateScriptUseCase: UpdateScriptUseCase,
        private val shortcutRepository: ShortcutRepository,
        private val iconRepository: IconRepository,
        private val categoryRepository: CategoryRepository,
        private val scriptRepository: ScriptRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModel() {
        private val _searchQuery = MutableStateFlow("")
        val searchQuery = _searchQuery.asStateFlow()

        private val _uiEvent = Channel<HomeUiEvent>()
        val uiEvent = _uiEvent.receiveAsFlow()
        private var pendingScript: Script? = null

        private val _selectedCategoryId = MutableStateFlow<Int?>(null)
        private val _sortOption = MutableStateFlow(SortOption.NAME_ASC)

        val selectedCategoryId = _selectedCategoryId.asStateFlow()
        val sortOption = _sortOption.asStateFlow()
        private var pendingArgs: String? = null
        private var pendingPrefix: String? = null
        private var pendingEnv: Map<String, String>? = null

        private val tileIdMappings: Flow<Map<Int, Int?>> =
            combine(
                (1..5).map { index ->
                    userPreferencesRepository.getScriptIdForTile(index).map { index to it }
                },
            ) { pairs -> pairs.toMap() }

        private val filterState =
            combine(
                _searchQuery,
                _selectedCategoryId,
                _sortOption,
            ) { query, categoryId, sort ->
                Triple(query, categoryId, sort)
            }

        val homeUiState: StateFlow<HomeUiState> =
            combine(
                scriptRepository.getAllScripts(),
                categoryRepository.getAllCategories(),
                tileIdMappings,
                filterState,
            ) { scripts, categories, tileIds, (query, selCatId, sort) ->
                var filteredList =
                    if (selCatId != null) {
                        scripts.filter { it.categoryId == selCatId }
                    } else {
                        scripts
                    }

                if (query.isNotBlank()) {
                    filteredList =
                        filteredList.filter {
                            it.name.contains(query, ignoreCase = true) ||
                                it.code.contains(query, ignoreCase = true)
                        }
                }

                val sortedList =
                    when (sort) {
                        SortOption.NAME_ASC -> filteredList.sortedBy { it.name.lowercase() }
                        SortOption.NAME_DESC -> filteredList.sortedByDescending { it.name.lowercase() }
                        SortOption.DATE_NEWEST -> filteredList.sortedByDescending { it.id }
                        SortOption.DATE_OLDEST -> filteredList.sortedBy { it.id }
                        SortOption.MANUAL -> filteredList.sortedBy { it.orderIndex }
                    }

                val finalTileMap =
                    tileIds.mapValues { (_, id) ->
                        if (id != null) scripts.find { it.id == id } else null
                    }

                HomeUiState.Success(
                    scripts = sortedList,
                    categories = categories,
                    tileMappings = finalTileMap,
                ) as HomeUiState
            }.onStart { emit(HomeUiState.Loading) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = HomeUiState.Loading,
                )

        fun onSearchQueryChange(newQuery: String) {
            _searchQuery.value = newQuery
        }

        suspend fun processImage(uri: Uri): String? = iconRepository.saveIcon(uri.toString())

        fun runScript(
            script: Script,
            runtimeArgs: String? = null,
            runtimePrefix: String? = null,
            runtimeEnv: Map<String, String>? = null,
        ) {
            viewModelScope.launch {
                try {
                    runScriptUseCase(
                        script = script,
                        runtimeArgs = runtimeArgs,
                        runtimePrefix = runtimePrefix,
                        runtimeEnv = runtimeEnv,
                    )
                    sendEvent(
                        HomeUiEvent.ShowSnackbar(
                            UiText.StringResource(R.string.msg_running_script, script.name),
                        ),
                    )
                    pendingScript = null
                    pendingArgs = null
                    pendingPrefix = null
                    pendingEnv = null
                } catch (_: TermuxPermissionException) {
                    pendingScript = script
                    pendingArgs = runtimeArgs
                    pendingPrefix = runtimePrefix
                    pendingEnv = runtimeEnv
                    sendEvent(HomeUiEvent.RequestTermuxPermission)
                } catch (e: TermuxException) {
                    sendEvent(HomeUiEvent.ShowSnackbar(e.uiText))
                } catch (e: Exception) {
                    sendEvent(HomeUiEvent.ShowSnackbar(UiText.DynamicString(e.message ?: "Error")))
                }
            }
        }

        fun deleteScript(script: Script) {
            viewModelScope.launch {
                deleteScriptUseCase(script)
                sendEvent(HomeUiEvent.ShowSnackbar(UiText.StringResource(R.string.msg_script_deleted)))
            }
        }

        fun updateScript(script: Script) {
            viewModelScope.launch {
                updateScriptUseCase(script)
                sendEvent(HomeUiEvent.ShowSnackbar(UiText.StringResource(R.string.msg_config_saved)))
            }
        }

        fun onPermissionResult(isGranted: Boolean) {
            if (isGranted) {
                pendingScript?.let { runScript(it, pendingArgs, pendingPrefix) }
            } else {
                sendEvent(HomeUiEvent.ShowSnackbar(UiText.StringResource(R.string.msg_permission_denied)))
                pendingScript = null
                pendingArgs = null
                pendingPrefix = null
                pendingEnv = null
            }
        }

        fun createShortcut(script: Script) {
            viewModelScope.launch {
                if (shortcutRepository.isPinningSupported()) {
                    val info = shortcutRepository.createShortcutInfo(script)
                    if (info != null) {
                        sendEvent(HomeUiEvent.CreateShortcut(info))
                    } else {
                        sendEvent(HomeUiEvent.ShowSnackbar(UiText.StringResource(R.string.error_shortcut_failed)))
                    }
                } else {
                    sendEvent(HomeUiEvent.ShowSnackbar(UiText.StringResource(R.string.error_pinning_not_supported)))
                }
            }
        }

        fun selectCategory(categoryId: Int?) {
            _selectedCategoryId.value = categoryId
        }

        fun setSortOption(option: SortOption) {
            _sortOption.value = option
        }

        private fun sendEvent(event: HomeUiEvent) {
            viewModelScope.launch { _uiEvent.send(event) }
        }

        fun moveScript(
            fromIndex: Int,
            toIndex: Int,
        ) {
            val currentState = homeUiState.value
            if (currentState !is HomeUiState.Success) return

            viewModelScope.launch {
                val list = currentState.scripts.toMutableList()
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)

                val updates =
                    list.mapIndexed { index, script ->
                        script.id to index
                    }

                scriptRepository.updateScriptsOrder(updates)
            }
        }

        fun addCategory(name: String) {
            viewModelScope.launch {
                categoryRepository.upsertCategory(Category(name = name))
            }
        }

        fun deleteCategory(category: Category) {
            viewModelScope.launch {
                val currentScripts = (homeUiState.value as? HomeUiState.Success)?.scripts ?: emptyList()
                val scriptsToNullify = currentScripts.filter { it.categoryId == category.id }

                scriptsToNullify.forEach { script ->
                    updateScriptUseCase(script.copy(categoryId = null))
                }

                categoryRepository.deleteCategory(category)

                if (_selectedCategoryId.value == category.id) {
                    _selectedCategoryId.value = null
                }
            }
        }
    }
