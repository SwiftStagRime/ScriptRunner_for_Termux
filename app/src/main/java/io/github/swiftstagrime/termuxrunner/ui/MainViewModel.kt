package io.github.swiftstagrime.termuxrunner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
import io.github.swiftstagrime.termuxrunner.ui.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _backStack = mutableListOf<NavKey>()
    val backStack = MutableStateFlow<List<NavKey>>(emptyList())

    init {
        viewModelScope.launch {
            userPreferencesRepository.hasCompletedOnboarding.collect { completed ->
                if (_backStack.isEmpty()) {
                    val start = if (completed) Route.Home else Route.Onboarding
                    setRoot(start)
                }
            }
        }
    }

    val useDynamicColors = userPreferencesRepository.useDynamicColors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

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