package io.github.swiftstagrime.termuxrunner.ui.features.runner

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.ui.components.ScriptRuntimePromptDialog
import io.github.swiftstagrime.termuxrunner.ui.theme.ScriptRunnerForTermuxTheme
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ScriptRunnerActivity : ComponentActivity() {

    private val viewModel: ScriptRunnerViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.onPermissionGranted()
        } else {
            Toast.makeText(
                this,
                getString(R.string.script_runner_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
            viewModel.dismissPrompt()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val accent by viewModel.selectedAccent.collectAsStateWithLifecycle()
            val mode by viewModel.selectedMode.collectAsStateWithLifecycle()
            val scriptToPrompt by viewModel.scriptToPrompt.collectAsStateWithLifecycle()

            ScriptRunnerForTermuxTheme(accent = accent, mode = mode) {
                scriptToPrompt?.let { script ->
                    ScriptRuntimePromptDialog(
                        script = script,
                        onDismiss = { viewModel.dismissPrompt() },
                        onConfirm = { args, prefix, env ->
                            viewModel.executeScript(
                                script,
                                runtimeArgs = args,
                                runtimePrefix = prefix,
                                runtimeEnv = env
                            )
                        }
                    )
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { handleEvent(it) }
            }
        }
    }

    private fun handleEvent(event: ScriptRunnerEvent) {
        when (event) {
            is ScriptRunnerEvent.Finish -> finish()
            is ScriptRunnerEvent.RequestPermission -> {
                requestPermissionLauncher.launch("com.termux.permission.RUN_COMMAND")
            }

            is ScriptRunnerEvent.ShowError -> {
                Toast.makeText(this, event.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}