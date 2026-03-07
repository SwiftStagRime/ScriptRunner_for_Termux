package io.github.swiftstagrime.termuxrunner.ui.features.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.swiftstagrime.termuxrunner.domain.repository.CategoryRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
import io.github.swiftstagrime.termuxrunner.ui.theme.AppTheme
import io.github.swiftstagrime.termuxrunner.ui.theme.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WidgetConfigurationViewModel
    @Inject
    constructor(
        private val scriptRepository: ScriptRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val categoryRepository: CategoryRepository,
    ) : ViewModel() {
        val selectedAccent =
            userPreferencesRepository.selectedAccent
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.GREEN)

        val selectedMode =
            userPreferencesRepository.selectedMode
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

        val allScripts =
            scriptRepository
                .getAllScripts()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val allCategories =
            categoryRepository
                .getAllCategories()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }
