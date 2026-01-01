package io.github.swiftstagrime.termuxrunner.ui.features.home

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.ui.components.ScriptConfigDialog
import io.github.swiftstagrime.termuxrunner.ui.preview.DevicePreviews
import io.github.swiftstagrime.termuxrunner.ui.preview.sampleScripts
import io.github.swiftstagrime.termuxrunner.ui.theme.ScriptRunnerForTermuxTheme
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onScriptCodeClick: (Script) -> Unit,
    onRunClick: (Script) -> Unit,
    onDeleteScript: (Script) -> Unit,
    onCreateShortcutClick: (Script) -> Unit,
    onUpdateScript: (Script) -> Unit,
    onHeartbeatToggle: (Boolean) -> Unit,
    isBatteryUnrestricted: Boolean,
    onRequestBatteryUnrestricted: () -> Unit,
    snackbarHostState: SnackbarHostState,
    onProcessImage: suspend (Uri) -> String?
) {
    var selectedScriptForConfig by remember { mutableStateOf<Script?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }
    val handleConfigClick: (Script) -> Unit = remember {
        { script -> selectedScriptForConfig = script }
    }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        onSearchQueryChange("")
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.primary
                    ),
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = {
                                Text(
                                    stringResource(R.string.search_placeholder),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSearchActive = false
                            onSearchQueryChange("")
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                stringResource(R.string.cd_close_search)
                            )
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, stringResource(R.string.cd_clear_search))
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = { Text(stringResource(R.string.home_title)) },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = stringResource(R.string.cd_search)
                            )
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = stringResource(R.string.cd_settings)
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_script))
            }
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (uiState) {
                is HomeUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                is HomeUiState.Success -> {
                    if (uiState.scripts.isEmpty()) {
                        val emptyText = if (searchQuery.isNotEmpty())
                            stringResource(R.string.empty_search_results)
                        else
                            stringResource(R.string.empty_scripts_list)

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emptyText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = uiState.scripts,
                                key = { it.id }
                            ) { script ->
                                ScriptItem(
                                    script = script,
                                    onCodeClick = onScriptCodeClick,
                                    onRunClick = onRunClick,
                                    onConfigClick = handleConfigClick,
                                    onDeleteClick = onDeleteScript,
                                    onCreateShortcutClick = onCreateShortcutClick
                                )
                            }
                        }
                    }
                }
            }
        }

        selectedScriptForConfig?.let { script ->
            ScriptConfigDialog(
                script = script,
                onDismiss = { selectedScriptForConfig = null },
                onSave = { updatedScript ->
                    onUpdateScript(updatedScript)
                    selectedScriptForConfig = null
                },
                onProcessImage = onProcessImage,
                onHeartbeatToggle = onHeartbeatToggle,
                isBatteryUnrestricted = isBatteryUnrestricted,
                onRequestBatteryUnrestricted = onRequestBatteryUnrestricted
            )
        }
    }
}

@Composable
private fun ScriptItem(
    script: Script,
    onCodeClick: (Script) -> Unit,
    onConfigClick: (Script) -> Unit,
    onRunClick: (Script) -> Unit,
    onDeleteClick: (Script) -> Unit,
    onCreateShortcutClick: (Script) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCodeClick(script) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScriptIcon(
                iconPath = script.iconPath,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = script.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = script.code.trim().take(50).replace("\n", " "),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(
                    onClick = { onRunClick(script) },
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Run",
                        modifier = Modifier.size(20.dp)
                    )
                }

                ScriptContextMenu(
                    script = script,
                    onConfigClick = onConfigClick,
                    onCreateShortcutClick = onCreateShortcutClick,
                    onDeleteClick = onDeleteClick
                )
            }
        }
    }
}

@Composable
private fun ScriptContextMenu(
    script: Script,
    onConfigClick: (Script) -> Unit,
    onCreateShortcutClick: (Script) -> Unit,
    onDeleteClick: (Script) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { showMenu = true }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "More",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 4.dp,
            modifier = Modifier.width(180.dp)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.menu_configuration),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Settings,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    showMenu = false
                    onConfigClick(script)
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.menu_pin_shortcut),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.PushPin,
                        null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    showMenu = false
                    onCreateShortcutClick(script)
                }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.menu_delete),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    showMenu = false
                    onDeleteClick(script)
                }
            )
        }
    }
}

@Composable
private fun ScriptIcon(iconPath: String?, modifier: Modifier = Modifier) {
    if (iconPath != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(File(iconPath))
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.cd_script_icon),
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@DevicePreviews
@Composable
private fun PreviewHomeScreen() {
    ScriptRunnerForTermuxTheme {
        HomeScreen(
            onRunClick = {},
            onAddClick = {},
            onSettingsClick = {},
            onScriptCodeClick = {},
            onDeleteScript = {},
            onCreateShortcutClick = {},
            onUpdateScript = {},
            uiState = HomeUiState.Success(sampleScripts),
            searchQuery = "",
            onSearchQueryChange = {},
            snackbarHostState = SnackbarHostState(),
            onProcessImage = { null },
            onHeartbeatToggle = {},
            isBatteryUnrestricted = false,
            onRequestBatteryUnrestricted = {}
        )
    }
}

@Preview(name = "Empty Home", showBackground = true)
@Composable
private fun PreviewEmptyHome() {
    ScriptRunnerForTermuxTheme {
        HomeScreen(
            onRunClick = {},
            onAddClick = {},
            onSettingsClick = {},
            onScriptCodeClick = {},
            onDeleteScript = {},
            onCreateShortcutClick = {},
            onUpdateScript = {},
            uiState = HomeUiState.Success(emptyList()),
            searchQuery = "",
            onSearchQueryChange = {},
            snackbarHostState = SnackbarHostState(),
            onProcessImage = { null },
            onHeartbeatToggle = {},
            isBatteryUnrestricted = false,
            onRequestBatteryUnrestricted = {}
        )
    }
}