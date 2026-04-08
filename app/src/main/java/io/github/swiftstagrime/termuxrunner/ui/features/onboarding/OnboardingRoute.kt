package io.github.swiftstagrime.termuxrunner.ui.features.onboarding

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingRoute(
    onSetupFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(pageCount = { 10 })
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        viewModel.checkStatus()
        if (isGranted) {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    OnboardingScreen(
        uiState = uiState,
        pagerState = pagerState,
        onNextClick = {
            if (pagerState.currentPage < pagerState.pageCount - 1) {
                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            } else {
                viewModel.completeOnboarding(onSetupFinished)
            }
        },
        onBackClick = {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
        },
        onGrantPermission = {
            permissionLauncher.launch("com.termux.permission.RUN_COMMAND")
        },
        onOpenTermuxSettings = {
            viewModel.requestTermuxOverlay()
        }
    )
}
