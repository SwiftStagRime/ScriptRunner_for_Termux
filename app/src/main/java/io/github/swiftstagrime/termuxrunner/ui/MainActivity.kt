package io.github.swiftstagrime.termuxrunner.ui

import android.animation.ObjectAnimator
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.service.quicksettings.TileService
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.SaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dagger.hilt.android.AndroidEntryPoint
import io.github.swiftstagrime.termuxrunner.data.service.tileIndexForComponent
import io.github.swiftstagrime.termuxrunner.ui.navigation.rememberEntryProvider
import io.github.swiftstagrime.termuxrunner.ui.theme.ScriptRunnerForTermuxTheme

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    companion object {
        private const val EXTRA_COMPONENT_NAME = "android.intent.extra.COMPONENT_NAME"
        private const val MAX_TILE_INDEX = 5
        const val EXTRA_OPEN_TILE_SETTINGS = "open_tile_settings"
        const val EXTRA_TILE_INDEX = "tile_index"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        handleTileIntent(intent)

        splashScreen.setKeepOnScreenCondition {
            !mainViewModel.isReady.value
        }
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            applyTerminalSlide(splashScreenView)
        }
        enableEdgeToEdge()

        setContent {
            val accent by mainViewModel.selectedAccent.collectAsStateWithLifecycle()
            val mode by mainViewModel.selectedMode.collectAsStateWithLifecycle()
            val customTheme by mainViewModel.customTheme.collectAsStateWithLifecycle()
            val backStack by mainViewModel.backStack.collectAsStateWithLifecycle()
            CompositionLocalProvider(
                LocalInspectionMode provides false,
            ) {
                ScriptRunnerForTermuxTheme(
                    accent = accent,
                    mode = mode,
                    customTheme = customTheme,
                ) {
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .semantics { testTagsAsResourceId = true },
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        val entryProvider = rememberEntryProvider(mainViewModel)
                        val saveableStateHolder = rememberSaveableStateHolder()
                        if (backStack.isNotEmpty()) {
                            NavDisplay(
                                backStack = backStack,
                                onBack = { mainViewModel.goBack() },
                                entryDecorators =
                                    listOf(
                                        remember(saveableStateHolder) {
                                            SaveableStateHolderNavEntryDecorator(saveableStateHolder)
                                        },
                                        rememberViewModelStoreNavEntryDecorator(),
                                    ),
                                entryProvider = entryProvider,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleTileIntent(intent)
    }

    private fun handleTileIntent(intent: Intent) {
        if (intent.action == TileService.ACTION_QS_TILE_PREFERENCES) {
            @Suppress("DEPRECATION")
            val tileComponent =
                intent.getParcelableExtra<ComponentName>(EXTRA_COMPONENT_NAME)

            val tileIndex = tileComponent?.let { tileIndexForComponent(it) } ?: return
            mainViewModel.openTileScriptEditor(tileIndex)
            return
        }

        if (intent.getBooleanExtra(EXTRA_OPEN_TILE_SETTINGS, false)) {
            val tileIndex = intent.getIntExtra(EXTRA_TILE_INDEX, -1)
            mainViewModel.openTileSettings(if (tileIndex in 1..MAX_TILE_INDEX) tileIndex else null)
        }
    }

    private fun applyTerminalSlide(splashProvider: SplashScreenViewProvider) {
        val splashScreenView = splashProvider.view

        val slideUp =
            ObjectAnimator.ofFloat(
                splashScreenView,
                View.TRANSLATION_Y,
                0f,
                -splashScreenView.height.toFloat(),
            )

        slideUp.apply {
            interpolator = OvershootInterpolator(1.2f)
            duration = 600L
            doOnEnd { splashProvider.remove() }
            start()
        }
    }
}
