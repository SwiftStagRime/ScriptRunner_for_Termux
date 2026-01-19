package io.github.swiftstagrime.termuxrunner.ui.features.home

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.model.Category
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.ui.components.ScriptIcon
import io.github.swiftstagrime.termuxrunner.ui.features.home.components.QuickSettingsBanner
import io.github.swiftstagrime.termuxrunner.ui.features.scriptconfigdialog.ScriptConfigDialog
import io.github.swiftstagrime.termuxrunner.ui.features.scriptconfigdialog.ScriptConfigState
import io.github.swiftstagrime.termuxrunner.ui.preview.DevicePreviews
import io.github.swiftstagrime.termuxrunner.ui.preview.sampleScripts
import io.github.swiftstagrime.termuxrunner.ui.preview.stubHomeActions
import io.github.swiftstagrime.termuxrunner.ui.theme.ScriptRunnerForTermuxTheme

data class HomeActions(
    val onSearchQueryChange: (String) -> Unit,
    val onOpenConfig: (Script) -> Unit,
    val onDismissConfig: () -> Unit,
    val onAddClick: () -> Unit,
    val onSettingsClick: () -> Unit,
    val onScriptCodeClick: (Script) -> Unit,
    val onRunClick: (Script) -> Unit,
    val onDeleteScript: (Script) -> Unit,
    val onCreateShortcutClick: (Script) -> Unit,
    val onUpdateScript: (Script) -> Unit,
    val onHeartbeatToggle: (Boolean) -> Unit,
    val onRequestBatteryUnrestricted: () -> Unit,
    val onRequestNotificationPermission: () -> Unit,
    val onProcessImage: suspend (Uri) -> String?,
    val onCategorySelect: (Int?) -> Unit,
    val onSortOptionChange: (SortOption) -> Unit,
    val onAddNewCategory: (String) -> Unit,
    val onDeleteCategory: (Category) -> Unit,
    val onMove: (Int, Int) -> Unit,
    val onTileSettingsClick: () -> Unit,
    val onNavigateToAutomation: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    searchQuery: String,
    configState: ScriptConfigState?,
    originalScript: Script?,
    isBatteryUnrestricted: Boolean,
    selectedCategoryId: Int?,
    sortOption: SortOption,
    snackbarHostState: SnackbarHostState,
    actions: HomeActions
) {
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        actions.onSearchQueryChange("")
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CollapsingHomeTopBar(
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                sortOption = sortOption,
                scrollBehavior = scrollBehavior,
                uiState = uiState,
                onToggleSearch = { isSearchActive = it },
                actions = actions
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = actions.onAddClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Add, stringResource(R.string.cd_add_script))
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().fadingEdge()) {
            when (uiState) {
                is HomeUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is HomeUiState.Success -> {
                    ScriptList(
                        uiState = uiState,
                        searchQuery = searchQuery,
                        selectedCategoryId = selectedCategoryId,
                        sortOption = sortOption,
                        onMove = actions.onMove,
                        onScriptCodeClick = actions.onScriptCodeClick,
                        onRunClick = actions.onRunClick,
                        onConfigClick = actions.onOpenConfig,
                        onDeleteClick = actions.onDeleteScript,
                        onCreateShortcutClick = actions.onCreateShortcutClick
                    )
                }
            }
        }

        if (configState != null && originalScript != null && uiState is HomeUiState.Success) {
            ScriptConfigDialog(
                state = configState,
                script = originalScript,
                categories = uiState.categories,
                onDismiss = actions.onDismissConfig,
                onSave = { updatedScript ->
                    actions.onUpdateScript(updatedScript)
                    actions.onDismissConfig()
                },
                onProcessImage = actions.onProcessImage,
                onHeartbeatToggle = actions.onHeartbeatToggle,
                isBatteryUnrestricted = isBatteryUnrestricted,
                onRequestBatteryUnrestricted = actions.onRequestBatteryUnrestricted,
                onAddNewCategory = actions.onAddNewCategory,
                onRequestNotificationPermission = actions.onRequestNotificationPermission,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollapsingHomeTopBar(
    isSearchActive: Boolean,
    searchQuery: String,
    sortOption: SortOption,
    scrollBehavior: TopAppBarScrollBehavior,
    uiState: HomeUiState,
    onToggleSearch: (Boolean) -> Unit,
    actions: HomeActions
) {
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        LargeTopAppBar(
            scrollBehavior = if (isSearchActive) null else scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                titleContentColor = MaterialTheme.colorScheme.onBackground
            ),
            title = {
                if (isSearchActive) {
                    SearchField(searchQuery, actions.onSearchQueryChange)
                } else {
                    Text(stringResource(R.string.home_title))
                }
            },
            navigationIcon = {
                if (isSearchActive) {
                    IconButton(onClick = {
                        onToggleSearch(false)
                        actions.onSearchQueryChange("")
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_close_search))
                    }
                }
            },
            actions = {
                if (isSearchActive) {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { actions.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, stringResource(R.string.cd_clear_search))
                        }
                    }
                } else {
                    AppBarActions(sortOption, actions, onToggleSearch)
                }
            }
        )

        if (uiState is HomeUiState.Success && !isSearchActive) {
            val fraction = scrollBehavior.state.collapsedFraction
            val easedAlpha = FastOutSlowInEasing.transform(1f - fraction)
            val easedScale = lerp(0.92f, 1f, easedAlpha)

            if (easedAlpha > 0.01f) {
                Column(
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = easedAlpha
                            scaleX = easedScale
                            scaleY = easedScale
                            translationY = scrollBehavior.state.heightOffset
                        }
                ) {
                    QuickSettingsBanner(uiState.tileMappings, actions.onRunClick, actions.onTileSettingsClick, actions.onTileSettingsClick)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
            CategoryTabs(uiState.categories, null, actions.onCategorySelect, actions.onDeleteCategory)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun Modifier.fadingEdge(): Modifier = this.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val fadeHeight = 10f
        val colors = listOf(Color.Transparent, Color.Black)

        drawRect(
            brush = Brush.verticalGradient(
                colors = colors,
                startY = 0f,
                endY = fadeHeight
            ),
            blendMode = BlendMode.DstIn
        )
    }
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.search_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AppBarActions(
    sortOption: SortOption,
    actions: HomeActions,
    onToggleSearch: (Boolean) -> Unit
) {
    IconButton(onClick = { onToggleSearch(true) }) {
        Icon(Icons.Default.Search, stringResource(R.string.cd_search))
    }
    IconButton(onClick = actions.onNavigateToAutomation) {
        Icon(Icons.Default.Schedule, stringResource(R.string.cd_automation))
    }
    SortMenu(currentSort = sortOption, onSortSelected = actions.onSortOptionChange)
    IconButton(onClick = actions.onSettingsClick) {
        Icon(Icons.Default.Settings, stringResource(R.string.cd_settings))
    }
}

@Composable
private fun ScriptList(
    uiState: HomeUiState.Success,
    searchQuery: String,
    selectedCategoryId: Int?,
    sortOption: SortOption,
    onMove: (Int, Int) -> Unit,
    onScriptCodeClick: (Script) -> Unit,
    onConfigClick: (Script) -> Unit,
    onRunClick: (Script) -> Unit,
    onDeleteClick: (Script) -> Unit,
    onCreateShortcutClick: (Script) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollPositions = rememberSaveable { mutableMapOf<String, Pair<Int, Int>>() }
    val scrollKey = remember(selectedCategoryId, searchQuery) {
        if (searchQuery.isNotEmpty()) "search_$searchQuery"
        else selectedCategoryId?.toString() ?: "ALL_LIST_PERSISTENCE_KEY"
    }
    val lazyListState = rememberLazyListState()
    val isManualSort = sortOption == SortOption.MANUAL

    var draggedItemIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var dragOffset by rememberSaveable { mutableFloatStateOf(0f) }

    LaunchedEffect(scrollKey) {
        val saved = scrollPositions[scrollKey]
        if (saved != null) {
            lazyListState.scrollToItem(saved.first, saved.second)
        } else {
            lazyListState.scrollToItem(0, 0)
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            if (lazyListState.isScrollInProgress) {
                lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset
            } else null
        }.collect { position ->
            if (position != null) {
                scrollPositions[scrollKey] = position
            }
        }
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(top = 0.dp, start = 12.dp, end = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize().then(
            if (isManualSort) {
                Modifier.pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            lazyListState.layoutInfo.visibleItemsInfo.find {
                                it.offset <= offset.y.toInt() && (it.offset + it.size) >= offset.y.toInt()
                            }?.let { draggedItemIndex = it.index }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount.y
                            val layoutInfo = lazyListState.layoutInfo
                            val itemInfo = layoutInfo.visibleItemsInfo
                            val currentItem = itemInfo.find { it.index == draggedItemIndex }
                            currentItem?.let {
                                val targetCenter = it.offset + (it.size / 2) + dragOffset
                                val targetItem = itemInfo.find { info -> targetCenter.toInt() in info.offset..(info.offset + info.size) }
                                if (targetItem != null && draggedItemIndex != null && targetItem.index != draggedItemIndex) {
                                    onMove(draggedItemIndex!!, targetItem.index)
                                    draggedItemIndex = targetItem.index
                                    dragOffset = 0f
                                }
                            }
                        },
                        onDragEnd = { draggedItemIndex = null; dragOffset = 0f },
                        onDragCancel = { draggedItemIndex = null; dragOffset = 0f }
                    )
                }
            } else Modifier
        )
    ) {
        val showHeaders = selectedCategoryId == null && searchQuery.isEmpty() && !isManualSort

        if (showHeaders) {
            uiState.scripts.groupBy { it.categoryId }.forEach { (catId, scripts) ->
                val catName = uiState.categories.find { it.id == catId }?.name ?: "Uncategorized"
                item(key = "header_$catId") { CategoryHeader(catName) }
                items(items = scripts, key = { it.id }) { script ->
                    ScriptItem(
                        script = script,
                        onCodeClick = onScriptCodeClick,
                        onConfigClick = onConfigClick,
                        onRunClick = onRunClick,
                        onDeleteClick = onDeleteClick,
                        onCreateShortcutClick = onCreateShortcutClick
                    )
                }
            }
        } else {
            itemsIndexed(items = uiState.scripts, key = { _, script -> script.id }) { index, script ->
                val isDragging = index == draggedItemIndex
                Box(modifier = Modifier.fillMaxWidth().graphicsLayer {
                    translationY = if (isDragging) dragOffset else 0f
                    scaleX = if (isDragging) 1.04f else 1.0f
                    scaleY = if (isDragging) 1.04f else 1.0f
                    alpha = if (isDragging) 0.9f else 1.0f
                }.zIndex(if (isDragging) 1f else 0f)) {
                    ScriptItem(
                        script = script,
                        onCodeClick = onScriptCodeClick,
                        onConfigClick = onConfigClick,
                        onRunClick = onRunClick,
                        onDeleteClick = onDeleteClick,
                        onCreateShortcutClick = onCreateShortcutClick
                    )
                }
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
    onCreateShortcutClick: (Script) -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onCodeClick(script) },
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScriptIcon(
                iconPath = script.iconPath,
                modifier = Modifier.size(56.dp),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = script.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text =
                        script.code
                            .trim()
                            .take(50)
                            .replace("\n", " "),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(
                    onClick = { onRunClick(script) },
                    modifier = Modifier.size(40.dp),
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Run",
                        modifier = Modifier.size(20.dp),
                    )
                }

                ScriptContextMenu(
                    script = script,
                    onConfigClick = onConfigClick,
                    onCreateShortcutClick = onCreateShortcutClick,
                    onDeleteClick = onDeleteClick,
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
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { showMenu = true }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.more),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 4.dp,
            modifier = Modifier.width(180.dp),
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.menu_configuration),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Settings,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = {
                    showMenu = false
                    onConfigClick(script)
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.menu_pin_shortcut),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.PushPin,
                        null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = {
                    showMenu = false
                    onCreateShortcutClick(script)
                },
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.menu_delete),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = {
                    showMenu = false
                    onDeleteClick(script)
                },
            )
        }
    }
}

@Composable
fun SortMenu(
    currentSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort))
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            shape = RoundedCornerShape(12),
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.manual_grouped)) },
                onClick = {
                    onSortSelected(SortOption.MANUAL)
                    showMenu = false
                },
                leadingIcon = {
                    if (currentSort == SortOption.MANUAL) {
                        Icon(
                            Icons.Default.DragIndicator,
                            stringResource(R.string.manual),
                        )
                    }
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.name_a_z)) },
                onClick = {
                    onSortSelected(SortOption.NAME_ASC)
                    showMenu = false
                },
                leadingIcon = {
                    if (currentSort == SortOption.NAME_ASC) {
                        Icon(
                            Icons.Default.SortByAlpha,
                            stringResource(R.string.a_z),
                        )
                    }
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.date_flat_list)) },
                onClick = {
                    onSortSelected(SortOption.DATE_NEWEST)
                    showMenu = false
                },
                leadingIcon = {
                    if (currentSort == SortOption.DATE_NEWEST) {
                        Icon(
                            Icons.Default.History,
                            stringResource(R.string.newest_by_date),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun CategoryHeader(name: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        modifier =
            Modifier
                .padding(top = 2.dp, bottom = 2.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
fun CategoryTabs(
    categories: List<Category>,
    selectedCategoryId: Int?,
    onCategorySelect: (Int?) -> Unit,
    onDeleteCategory: (Category) -> Unit,
) {
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            CategoryChip(
                label = "All",
                isSelected = selectedCategoryId == null,
                onClick = { onCategorySelect(null) },
            )
        }

        items(categories, key = { it.id }) { category ->
            CategoryChip(
                label = category.name,
                isSelected = selectedCategoryId == category.id,
                onClick = { onCategorySelect(category.id) },
                onLongClick = { categoryToEdit = category },
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
                        cat.name,
                    ),
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
            shape = RoundedCornerShape(28.dp),
        )
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    var isPressed by remember { mutableStateOf(false) }

    val backgroundColor =
        when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            isPressed -> MaterialTheme.colorScheme.surfaceContainerHighest
            else -> MaterialTheme.colorScheme.surfaceContainer
        }

    val contentColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    val borderColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        }

    Surface(
        modifier =
            Modifier
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
                        },
                    )
                },
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@DevicePreviews
@Composable
private fun PreviewHomeScreen() {
    ScriptRunnerForTermuxTheme {
        HomeScreen(
            uiState = HomeUiState.Success(sampleScripts, emptyList(), emptyMap()),
            searchQuery = "",
            configState = null,
            originalScript = null,
            isBatteryUnrestricted = false,
            selectedCategoryId = null,
            sortOption = SortOption.NAME_ASC,
            snackbarHostState = SnackbarHostState(),
            actions = stubHomeActions
        )
    }
}

@Preview(name = "Empty Home", showBackground = true)
@Composable
private fun PreviewEmptyHome() {
    ScriptRunnerForTermuxTheme {
        HomeScreen(
            uiState = HomeUiState.Success(emptyList(), emptyList(), emptyMap()),
            searchQuery = "",
            configState = null,
            originalScript = null,
            isBatteryUnrestricted = false,
            selectedCategoryId = null,
            sortOption = SortOption.NAME_ASC,
            snackbarHostState = SnackbarHostState(),
            actions = stubHomeActions
        )
    }
}
