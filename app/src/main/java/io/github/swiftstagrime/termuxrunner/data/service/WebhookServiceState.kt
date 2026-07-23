package io.github.swiftstagrime.termuxrunner.data.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebhookServiceState
    @Inject
    constructor() {
        private val _isRunning = MutableStateFlow(false)

        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun setRunning(running: Boolean) {
            if (_isRunning.value != running) {
                _isRunning.value = running
            }
        }
    }
