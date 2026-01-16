package io.github.swiftstagrime.termuxrunner.ui.features.home

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.model.Category
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.ui.components.ScriptConfigDialog
import io.github.swiftstagrime.termuxrunner.ui.components.ScriptIcon
import io.github.swiftstagrime.termuxrunner.ui.features.home.components.QuickSettingsBanner
import io.github.swiftstagrime.termuxrunner.ui.preview.DevicePreviews
import io.github.swiftstagrime.termuxrunner.ui.preview.sampleScripts
import io.github.swiftstagrime.termuxrunner.ui.theme.ScriptRunnerForTermuxTheme

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
    onRequestNotificationPermission: () -> Unit,
    snackbarHostState: SnackbarHostState,
    onProcessImage: suspend (Uri) -> String?,
    selectedCategoryId: Int?,
    sortOption: SortOption,
    onCategorySelect: (Int?) -> Unit,
    onSortOptionChange: (SortOption) -> Unit,
    onAddNewCategory: (String) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onMove: (Int, Int) -> Unit,
    onTileSettingsClick: () -> Unit,
    onNavigateToAutomation: () -> Unit
) {
    var selectedScriptForConfig by remember { mutableStateOf<Script?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }
    val handleConfigClick: (Script) -> Unit = remember {
        { script -> selectedScriptForConfig = script }
    }
    val lazyListState = rememberLazyListState()
    val isManualSort = sortOption == SortOption.MANUAL

    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    val uncategorizedLabel = stringResource(R.string.uncategorized)

    fun getTargetIndex(offset: Float, startBuffer: Int = 0): Int? {
        val layoutInfo = lazyListState.layoutInfo
        val itemInfo = layoutInfo.visibleItemsInfo
        val currentDraggingItem = itemInfo.find { it.index == draggedItemIndex } ?: return null

        val targetCenter = currentDraggingItem.offset + (currentDraggingItem.size / 2) + offset
        return itemInfo.find { targetCenter.toInt() in it.offset..(it.offset + it.size) }?.index
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
                        IconButton(onClick = onNavigateToAutomation) {
                            Icon(Icons.Default.Schedule, stringResource(R.string.cd_automation))
                        }
                        SortMenu(
                            currentSort = sortOption,
                            onSortSelected = onSortOptionChange
                        )
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
        Column(modifier = Modifier.padding(padding)) {
            if (uiState is HomeUiState.Success) {
                if (!isSearchActive) {
                    QuickSettingsBanner(
                        tileMappings = uiState.tileMappings,
                        onTileClick = onRunClick,
                        onEmptyTileClick = onTileSettingsClick,
                        onSettingsClick = onTileSettingsClick
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                CategoryTabs(
                    categories = uiState.categories,
                    selectedCategoryId = selectedCategoryId,
                    onCategorySelect = onCategorySelect,
                    onDeleteCategory = onDeleteCategory
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (uiState) {
                    is HomeUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    is HomeUiState.Success -> {
                        LazyColumn(
                            state = lazyListState,
                            contentPadding = PaddingValues(
                                top = 16.dp,
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 88.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (isManualSort) {
                                        Modifier.pointerInput(Unit) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { offset ->
                                                    lazyListState.layoutInfo.visibleItemsInfo
                                                        .find { item ->
                                                            offset.y.toInt() in item.offset..(item.offset + item.size)
                                                        }?.let { draggedItemIndex = it.index }
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffset += dragAmount.y

                                                    val targetIndex = getTargetIndex(dragOffset)
                                                    if (targetIndex != null && draggedItemIndex != null && targetIndex != draggedItemIndex) {
                                                        onMove(draggedItemIndex!!, targetIndex)
                                                        draggedItemIndex = targetIndex
                                                        dragOffset = 0f
                                                    }
                                                },
                                                onDragEnd = {
                                                    draggedItemIndex = null
                                                    dragOffset = 0f
                                                },
                                                onDragCancel = {
                                                    draggedItemIndex = null
                                                    dragOffset = 0f
                                                }
                                            )
                                        }
                                    } else Modifier
                                )
                        ) {
                            // Only show headers if NOT in manual sort mode
                            // (Dragging across headers causes UI glitches without complex math) | but it does glitch anyway ;)
                            val showHeaders =
                                selectedCategoryId == null && searchQuery.isEmpty() && !isManualSort

                            if (showHeaders) {
                                val grouped = uiState.scripts.groupBy { it.categoryId }
                                grouped.forEach { (catId, scripts) ->
                                    val catName = uiState.categories.find { it.id == catId }?.name
                                        ?: uncategorizedLabel
                                    item(key = "header_$catId") { CategoryHeader(catName) }
                                    items(items = scripts, key = { it.id }) { script ->
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
                            } else {
                                itemsIndexed(
                                    items = uiState.scripts,
                                    key = { _, script -> script.id }
                                ) { index, script ->
                                    val isDragging = index == draggedItemIndex

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .graphicsLayer {
                                                translationY = if (isDragging) dragOffset else 0f
                                                scaleX = if (isDragging) 1.05f else 1.0f
                                                scaleY = if (isDragging) 1.05f else 1.0f
                                                alpha = if (isDragging) 0.8f else 1.0f
                                            }
                                            .zIndex(if (isDragging) 1f else 0f)
                                    ) {
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
            }

            selectedScriptForConfig?.let { script ->
                ScriptConfigDialog(
                    script = script,
                    categories = (uiState as? HomeUiState.Success)?.categories ?: emptyList(),
                    onDismiss = { selectedScriptForConfig = null },
                    onSave = { updatedScript ->
                        onUpdateScript(updatedScript)
                        selectedScriptForConfig = null
                    },
                    onProcessImage = onProcessImage,
                    onHeartbeatToggle = onHeartbeatToggle,
                    isBatteryUnrestricted = isBatteryUnrestricted,
                    onRequestBatteryUnrestricted = onRequestBatteryUnrestricted,
                    onAddNewCategory = onAddNewCategory,
                    onRequestNotificationPermission = onRequestNotificationPermission
                )
            }
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
                contentDescription = stringResource(R.string.more),
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
fun SortMenu(
    currentSort: SortOption,
    onSortSelected: (SortOption) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort))
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            shape = RoundedCornerShape(12)
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.manual_grouped)) },
                onClick = { onSortSelected(SortOption.MANUAL); showMenu = false },
                leadingIcon = {
                    if (currentSort == SortOption.MANUAL) Icon(
                        Icons.Default.DragIndicator,
                        stringResource(R.string.manual)
                    )
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.name_a_z)) },
                onClick = { onSortSelected(SortOption.NAME_ASC); showMenu = false },
                leadingIcon = {
                    if (currentSort == SortOption.NAME_ASC) Icon(
                        Icons.Default.SortByAlpha,
                        stringResource(R.string.a_z)
                    )
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.date_flat_list)) },
                onClick = { onSortSelected(SortOption.DATE_NEWEST); showMenu = false },
                leadingIcon = {
                    if (currentSort == SortOption.DATE_NEWEST) Icon(
                        Icons.Default.History,
                        stringResource(R.string.newest_by_date)
                    )
                }
            )
        }
    }
}

@Composable
private fun CategoryHeader(name: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .padding(top = 2.dp, bottom = 2.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun CategoryTabs(
    categories: List<Category>,
    selectedCategoryId: Int?,
    onCategorySelect: (Int?) -> Unit,
    onDeleteCategory: (Category) -> Unit
) {
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            CategoryChip(
                label = "All",
                isSelected = selectedCategoryId == null,
                onClick = { onCategorySelect(null) }
            )
        }

        items(categories, key = { it.id }) { category ->
            CategoryChip(
                label = category.name,
                isSelected = selectedCategoryId == category.id,
                onClick = { onCategorySelect(category.id) },
                onLongClick = { categoryToEdit = category }
            )
        }
    }

    categoryToEdit?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToEdit = null },
            title = { Text(stringResource(R.string.delete_category)) },
            text = {
                Text(
                    stringResource(
                        R.string.scripts_in_will_be_moved_to_uncategorized_this_cannot_be_undone,
                        cat.name
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCategory(cat)
                    categoryToEdit = null
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    categoryToEdit = null
                }) { Text(stringResource(R.string.cancel)) }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    var isPressed by remember { mutableStateOf(false) }

    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isPressed -> MaterialTheme.colorScheme.surfaceContainerHighest
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    val contentColor = if (isSelected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    val borderColor = if (isSelected)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    else
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        try {
                            isPressed = true
                            awaitRelease()
                        } finally {
                            isPressed = false
                        }
                    },
                    onTap = { onClick() },
                    onLongPress = {
                        isPressed = false
                        onLongClick?.invoke()
                    }
                )
            },
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
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
            uiState = HomeUiState.Success(sampleScripts, emptyList(), emptyMap()),
            searchQuery = "",
            onSearchQueryChange = {},
            snackbarHostState = SnackbarHostState(),
            onProcessImage = { null },
            onHeartbeatToggle = {},
            isBatteryUnrestricted = false,
            onRequestBatteryUnrestricted = {},
            onCategorySelect = {},
            onSortOptionChange = {},
            sortOption = SortOption.NAME_ASC,
            selectedCategoryId = null,
            onAddNewCategory = {},
            onDeleteCategory = {},
            onMove = { _, _ -> },
            onRequestNotificationPermission = {},
            onTileSettingsClick = {},
            onNavigateToAutomation = {}
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
            uiState = HomeUiState.Success(emptyList(), emptyList(), emptyMap()),
            searchQuery = "",
            onSearchQueryChange = {},
            snackbarHostState = SnackbarHostState(),
            onProcessImage = { null },
            onHeartbeatToggle = {},
            isBatteryUnrestricted = false,
            onRequestBatteryUnrestricted = {},
            onCategorySelect = {},
            onSortOptionChange = {},
            sortOption = SortOption.NAME_ASC,
            selectedCategoryId = null,
            onAddNewCategory = {},
            onDeleteCategory = {},
            onMove = { _, _ -> },
            onRequestNotificationPermission = {},
            onTileSettingsClick = {},
            onNavigateToAutomation = {}
        )
    }
}