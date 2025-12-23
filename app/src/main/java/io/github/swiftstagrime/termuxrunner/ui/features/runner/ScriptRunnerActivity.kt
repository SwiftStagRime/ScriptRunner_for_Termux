package io.github.swiftstagrime.termuxrunner.ui.features.runner

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.swiftstagrime.termuxrunner.R
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ScriptRunnerActivity : ComponentActivity() {

    private val viewModel: ScriptRunnerViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.executeScript()
        } else {
            Toast.makeText(
                this,
                getString(R.string.script_runner_permission_denied), Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    handleEvent(event)
                }
            }
        }
    }

    private fun handleEvent(event: ScriptRunnerEvent) {
        when (event) {
            is ScriptRunnerEvent.Finish -> {
                finish()
            }

            is ScriptRunnerEvent.RequestPermission -> {
                requestPermissionLauncher.launch("com.termux.permission.RUN_COMMAND")
            }

            is ScriptRunnerEvent.ShowError -> {
                Toast.makeText(this, event.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}