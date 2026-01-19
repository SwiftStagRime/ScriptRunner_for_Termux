package io.github.swiftstagrime.termuxrunner.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AnticipateInterpolator
import android.view.animation.OvershootInterpolator
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
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
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
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
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
            val backStack by mainViewModel.backStack.collectAsStateWithLifecycle()

            ScriptRunnerForTermuxTheme(accent = accent, mode = mode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
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

    private fun applyTerminalSlide(splashProvider: SplashScreenViewProvider) {
        val splashScreenView = splashProvider.view

        val slideUp = ObjectAnimator.ofFloat(
            splashScreenView,
            View.TRANSLATION_Y,
            0f,
            -splashScreenView.height.toFloat()
        )

        slideUp.apply {
            interpolator = OvershootInterpolator(1.2f)
            duration = 600L
            doOnEnd { splashProvider.remove() }
            start()
        }
    }
}

