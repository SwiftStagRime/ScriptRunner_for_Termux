package io.github.swiftstagrime.termuxrunner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import io.github.swiftstagrime.termuxrunner.ui.MainViewModel
import io.github.swiftstagrime.termuxrunner.ui.features.editor.EditorRoute
import io.github.swiftstagrime.termuxrunner.ui.features.editor.EditorViewModel
import io.github.swiftstagrime.termuxrunner.ui.features.home.HomeRoute
import io.github.swiftstagrime.termuxrunner.ui.features.onboarding.OnboardingRoute
import io.github.swiftstagrime.termuxrunner.ui.features.settings.SettingsRoute
import io.github.swiftstagrime.termuxrunner.ui.features.tiles.TileSettingsRoute


@Composable
fun rememberEntryProvider(
    mainViewModel: MainViewModel
): (NavKey) -> NavEntry<NavKey> {
    return remember(mainViewModel) {
        { key ->
            NavEntry(key) {
                when (key) {
                    is Route.Onboarding -> {
                        OnboardingRoute(
                            onSetupFinished = { mainViewModel.replaceRoot(Route.Home) }
                        )
                    }

                    is Route.Home -> {
                        HomeRoute(
                            onNavigateToEditor = { scriptId ->
                                mainViewModel.navigateTo(Route.Editor(scriptId))
                            },
                            onNavigateToSettings = {
                                mainViewModel.navigateTo(Route.Settings)
                            },
                            onNavigateToTileSettings = {
                                mainViewModel.navigateTo(Route.TileSettings)
                            }
                        )
                    }

                    is Route.Editor -> {

                        val viewModel: EditorViewModel = hiltViewModel()

                        LaunchedEffect(key.scriptId) {
                            viewModel.loadScript(key.scriptId)
                        }

                        EditorRoute(
                            onBack = { mainViewModel.goBack() },
                            viewModel = viewModel
                        )
                    }

                    is Route.Settings -> {
                        SettingsRoute(
                            onBack = { mainViewModel.goBack() },
                            onNavigateToEditor = { scriptId ->
                                mainViewModel.navigateTo(Route.Editor(scriptId))
                            }
                        )
                    }

                    is Route.TileSettings -> {
                        TileSettingsRoute(
                            onBack = { mainViewModel.goBack() }
                        )
                    }


                    else -> {
                    }
                }
            }
        }
    }
}