package io.github.swiftstagrime.termuxrunner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
import io.github.swiftstagrime.termuxrunner.ui.navigation.Route
import io.github.swiftstagrime.termuxrunner.ui.theme.AppTheme
import io.github.swiftstagrime.termuxrunner.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    private val _backStack = mutableListOf<NavKey>()
    val backStack = MutableStateFlow<List<NavKey>>(emptyList())

    val selectedAccent = userPreferencesRepository.selectedAccent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.GREEN)

    val selectedMode = userPreferencesRepository.selectedMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    init {
        viewModelScope.launch {
            userPreferencesRepository.hasCompletedOnboarding.take(1).collect { completed ->
                if (_backStack.isEmpty()) {
                    val start = if (completed) Route.Home else Route.Onboarding
                    setRoot(start)
                }
                _isReady.value = true
            }
        }
    }

    fun navigateTo(key: NavKey) {
        val current = _backStack.toMutableList()
        current.add(key)
        updateStack(current)
    }

    fun goBack() {
        val current = _backStack.toMutableList()
        if (current.size > 1) {
            current.removeAt(current.lastIndex)
            updateStack(current)
        }
    }

    fun replaceRoot(key: NavKey) {
        updateStack(listOf(key))
    }

    private fun setRoot(key: NavKey) {
        updateStack(listOf(key))
    }

    private fun updateStack(newStack: List<NavKey>) {
        _backStack.clear()
        _backStack.addAll(newStack)
        backStack.value = newStack.toList()
    }
}
