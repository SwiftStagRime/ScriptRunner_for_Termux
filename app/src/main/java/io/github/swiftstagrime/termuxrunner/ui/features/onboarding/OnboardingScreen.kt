package io.github.swiftstagrime.termuxrunner.ui.features.onboarding

import android.content.ClipData
import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.ui.components.LanguageSelectorIcon
import io.github.swiftstagrime.termuxrunner.ui.features.automation.AutomationScreenPreview
import io.github.swiftstagrime.termuxrunner.ui.features.editor.PreviewEditorNewRaw
import io.github.swiftstagrime.termuxrunner.ui.features.home.PreviewHomeScreen
import io.github.swiftstagrime.termuxrunner.ui.features.settings.PreviewSettingsScreen
import io.github.swiftstagrime.termuxrunner.ui.features.tiles.TileSettingsScreenPreview
import io.github.swiftstagrime.termuxrunner.ui.preview.DevicePreviews
import io.github.swiftstagrime.termuxrunner.ui.theme.ScriptRunnerForTermuxTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    pagerState: PagerState,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    onGrantPermission: () -> Unit,
    onOpenTermuxSettings: () -> Unit,
) {
    val outerBackgroundColor = MaterialTheme.colorScheme.surface
    val sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest

    Scaffold(
        containerColor = outerBackgroundColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.onboarding_setup_title),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = outerBackgroundColor,
                ),
                actions = { LanguageSelectorIcon() }
            )
        },
    ) { padding ->
        Surface(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 8.dp)
                .padding(bottom = 8.dp)
                .fillMaxSize(),
            color = sheetContainerColor,
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    userScrollEnabled = false,
                    verticalAlignment = Alignment.Top
                ) { pageIndex ->
                    OnboardingPageContent(
                        pageIndex = pageIndex,
                        uiState = uiState,
                        onGrantPermission = onGrantPermission,
                        onOpenTermuxSettings = onOpenTermuxSettings
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PagerIndicator(
                        count = pagerState.pageCount,
                        currentPage = pagerState.currentPage
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (pagerState.currentPage > 0) {
                            OutlinedButton(
                                onClick = onBackClick,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                Text(stringResource(R.string.back_label))
                            }
                        }

                        val isLastPage = pagerState.currentPage == pagerState.pageCount - 1
                        Button(
                            onClick = onNextClick,
                            modifier = Modifier.weight(2f),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Text(
                                text = if (isLastPage) stringResource(R.string.finish_setup_label)
                                else stringResource(R.string.next_label)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    pageIndex: Int,
    uiState: OnboardingUiState,
    onGrantPermission: () -> Unit,
    onOpenTermuxSettings: () -> Unit,
) {
    when (pageIndex) {
        0 -> WelcomeStep()
        1 -> TermuxInstallStep(uiState.isTermuxInstalled)
        2 -> TermuxConfigStep()
        3 -> PermissionStep(uiState.isPermissionGranted, onGrantPermission)
        4 -> OptimizationStep(uiState.isBatteryUnrestricted, onOpenTermuxSettings)

        5 -> FeatureShowcaseStep(
            title = "Manage Your Scripts",
            description = "PLACEHOLDER",
            preview = { PreviewHomeScreen() }
        )
        6 -> FeatureShowcaseStep(
            title = "Powerful Editor",
            description = "PLACEHOLDER",
            preview = { PreviewEditorNewRaw() }
        )
        7 -> FeatureShowcaseStep(
            title = "Automation",
            description = "PLACEHOLDER",
            preview = { AutomationScreenPreview() }
        )
        8 -> FeatureShowcaseStep(
            title = "Quick Access Tiles",
            description = "PLACEHOLDER",
            preview = { TileSettingsScreenPreview() }
        )
        9 -> FeatureShowcaseStep(
            title = "Fully Customizable",
            description = "PLACEHOLDER",
            preview = { PreviewSettingsScreen() }
        )
    }
}

@Composable
fun TermuxInstallStep(isInstalled: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StepIcon(Icons.Default.InstallMobile, isDone = isInstalled)
        Text(stringResource(R.string.setup_step_1_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.setup_step_1_desc), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (!isInstalled) {
            RequirementWarning(message = "Termux not found. Please install it to continue.")
        }
    }
}

@Composable
fun TermuxConfigStep() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StepIcon(Icons.Default.SettingsSuggest, isDone = false)
        Text(stringResource(R.string.setup_step_2_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.setup_step_2_desc), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)

        CodeBlock(code = stringResource(R.string.setup_step_2_code))
        CodeBlock(code = stringResource(R.string.setup_step_2_code_2))
    }
}

@Composable
fun PermissionStep(isPermissionGranted: Boolean, onGrantPermission: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StepIcon(Icons.Default.Security, isDone = isPermissionGranted)
        Text(stringResource(R.string.setup_step_3_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.setup_step_3_desc), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (!isPermissionGranted) {
            RequirementWarning(message = "This permission is essential for the app to function.")
            Button(onClick = onGrantPermission, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Text(stringResource(R.string.grant_permission_label))
            }
        }
    }
}

@Composable
fun OptimizationStep(isBatteryUnrestricted: Boolean, onOpenSettings: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StepIcon(Icons.Default.BatteryChargingFull, isDone = isBatteryUnrestricted)
        Text(stringResource(R.string.setup_step_5_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.setup_step_5_desc), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (!isBatteryUnrestricted) {
            RequirementWarning(message = stringResource(R.string.battery_warning_text))
        }

        Button(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Text(stringResource(R.string.open_termux_settings))
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(18.dp))
        }

        Text(
            text = stringResource(R.string.setup_step_4_desc),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun PagerIndicator(count: Int, currentPage: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { it ->
            val isSelected = it == currentPage
            Box(
                modifier = Modifier
                    .size(if (isSelected) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

@Composable
fun StepIcon(
    imageVector: ImageVector,
    isDone: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isDone) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "icon_background"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isDone) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "icon_color"
    )

    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = isDone, label = "icon_fade") { done ->
            if (done) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = iconColor
                )
            } else {
                Icon(
                    imageVector = imageVector,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = iconColor
                )
            }
        }
    }
}

@Composable
fun WelcomeStep() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(R.string.onboarding_welcome_desc),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Simple. Powerful. Automated.",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CodeBlock(
    code: String,
    onCopy: (String) -> Unit = {},
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SelectionContainer(modifier = Modifier.weight(1f)) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                )
            }
            IconButton(onClick = {
                scope.launch {
                    val clipData = ClipData.newPlainText("Termux Script", code)
                    clipboard.setClipEntry(ClipEntry(clipData))
                    onCopy(code)
                }
            }) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.copy_clipboard_description),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
fun RequirementWarning(
    message: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
fun DeviceFrame(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(0.85f)
            .aspectRatio(9f / 19f)
            .shadow(16.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(6.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(modifier = Modifier.pointerInput(Unit) {}) {
            content()
        }
    }
}

@Composable
fun FeatureShowcaseStep(
    title: String,
    description: String,
    preview: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        DeviceFrame(modifier = Modifier.weight(1f)) {
            preview()
        }

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
        )
    }
}



@Composable
fun EducationStep() {
    Text(
        text = "How it works",
        style = MaterialTheme.typography.headlineSmall
    )
    Spacer(modifier = Modifier.height(16.dp))

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
            Text("1. Add a Script Path", fontWeight = FontWeight.Bold)
            Text("2. Select an Icon", fontWeight = FontWeight.Bold)
            Text("3. Run from Home Screen", fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview(name = "Welcome Step", showBackground = true)
@Composable
private fun PreviewOnboardingWelcome() {
    ScriptRunnerForTermuxTheme {
        OnboardingScreen(
            uiState = OnboardingUiState(
                isTermuxInstalled = false,
                isPermissionGranted = false,
                isBatteryUnrestricted = false
            ),
            pagerState = rememberPagerState(pageCount = { 5 }, initialPage = 0),
            onNextClick = {},
            onBackClick = {},
            onGrantPermission = {},
            onOpenTermuxSettings = {}
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview(name = "Install Step - Requirement Missing", showBackground = true, device = "id:pixel_5")
@Composable
private fun PreviewOnboardingInstallMissing() {
    ScriptRunnerForTermuxTheme {
        OnboardingScreen(
            uiState = OnboardingUiState(
                isTermuxInstalled = false,
                isPermissionGranted = false
            ),
            pagerState = rememberPagerState(pageCount = { 5 }, initialPage = 1),
            onNextClick = {},
            onBackClick = {},
            onGrantPermission = {},
            onOpenTermuxSettings = {}
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview(name = "Permission Step - Granted", showBackground = true, device = "id:pixel_5")
@Composable
private fun PreviewOnboardingPermissionGranted() {
    ScriptRunnerForTermuxTheme {
        OnboardingScreen(
            uiState = OnboardingUiState(
                isTermuxInstalled = true,
                isPermissionGranted = true
            ),
            pagerState = rememberPagerState(pageCount = { 5 }, initialPage = 2),
            onNextClick = {},
            onBackClick = {},
            onGrantPermission = {},
            onOpenTermuxSettings = {}
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview(name = "Optimization Step", showBackground = true, device = "id:pixel_5")
@Composable
private fun PreviewOnboardingOptimization() {
    ScriptRunnerForTermuxTheme() {
        OnboardingScreen(
            uiState = OnboardingUiState(
                isTermuxInstalled = true,
                isPermissionGranted = true,
                isBatteryUnrestricted = false
            ),
            pagerState = rememberPagerState(pageCount = { 5 }, initialPage = 4),
            onNextClick = {},
            onBackClick = {},
            onGrantPermission = {},
            onOpenTermuxSettings = {}
        )
    }
}