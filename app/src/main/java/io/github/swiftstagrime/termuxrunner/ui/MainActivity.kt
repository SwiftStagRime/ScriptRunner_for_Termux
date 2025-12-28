package io.github.swiftstagrime.termuxrunner.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.SaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dagger.hilt.android.AndroidEntryPoint
import io.github.swiftstagrime.termuxrunner.ui.navigation.rememberEntryProvider
import io.github.swiftstagrime.termuxrunner.ui.theme.ScriptRunnerForTermuxTheme

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val useDynamicColors by mainViewModel.useDynamicColors.collectAsStateWithLifecycle()
            val backStack by mainViewModel.backStack.collectAsStateWithLifecycle()

            ScriptRunnerForTermuxTheme(dynamicColor = useDynamicColors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val entryProvider = rememberEntryProvider(mainViewModel)
                    val saveableStateHolder = rememberSaveableStateHolder()
                    if (backStack.isNotEmpty()) {
                        NavDisplay(
                            backStack = backStack,

                            onBack = { mainViewModel.goBack() },

                            entryDecorators = listOf(
                                remember(saveableStateHolder) {
                                    SaveableStateHolderNavEntryDecorator(saveableStateHolder)
                                },

                                rememberViewModelStoreNavEntryDecorator()
                            ),

                            entryProvider = entryProvider
                        )
                    }
                }
            }
        }
    }
}