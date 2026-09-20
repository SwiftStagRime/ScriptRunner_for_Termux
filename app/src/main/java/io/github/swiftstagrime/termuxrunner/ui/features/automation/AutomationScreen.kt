package io.github.swiftstagrime.termuxrunner.ui.features.automation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.model.Automation
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import io.github.swiftstagrime.termuxrunner.ui.components.ScriptIcon
import io.github.swiftstagrime.termuxrunner.ui.features.home.SortMenu
import io.github.swiftstagrime.termuxrunner.ui.features.home.SortOption
import io.github.swiftstagrime.termuxrunner.ui.preview.DevicePreviews
import io.github.swiftstagrime.termuxrunner.ui.preview.mockAutomations

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AutomationScreen(
    uiState: AutomationUiState,
    sortOption: SortOption,
    onBackClick: () -> Unit,
    onToggleAutomation: (Int, Boolean) -> Unit,
    onDeleteAutomation: (Automation) -> Unit,
    onAddAutomationClick: () -> Unit,
    onRunNow: (Automation) -> Unit,
    onShowHistory: (Automation) -> Unit,
    onRequestPermission: () -> Unit,
    onEditChain: (Automation) -> Unit,
    onEditAutomation: (Automation) -> Unit,
    onSortOptionChange: (SortOption) -> Unit,
    onMoveAutomation: (Int, Int) -> Unit,
) {
    val outerBackgroundColor = MaterialTheme.colorScheme.surface
    val sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest

    Scaffold(
        containerColor = outerBackgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.automation_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    SortMenu(currentSort = sortOption, onSortSelected = onSortOptionChange)
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
                onClick = onAddAutomationClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
            ) {
                Icon(Icons.Default.AddAlarm, null)
            }
        },
    ) { padding ->
        Surface(
            modifier =
                Modifier
                    .padding(padding)
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 8.dp)
                    .fillMaxSize(),
            color = sheetContainerColor,
            shape =
                RoundedCornerShape(
                    topStart = 32.dp,
                    topEnd = 32.dp,
                    bottomEnd = 32.dp,
                    bottomStart = 32.dp,
                ),
            shadowElevation = 1.dp,
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))

                if (!uiState.isExactAlarmPermissionGranted) {
                    PermissionWarningBanner(onClick = onRequestPermission)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                AutomationList(
                    items = uiState.items,
                    sortOption = sortOption,
                    onToggle = onToggleAutomation,
                    onDelete = onDeleteAutomation,
                    onRunNow = onRunNow,
                    onShowHistory = onShowHistory,
                    onEditChain = onEditChain,
                    onEdit = onEditAutomation,
                    onMove = onMoveAutomation,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AutomationList(
    items: List<AutomationUiItem>,
    sortOption: SortOption,
    onToggle: (Int, Boolean) -> Unit,
    onDelete: (Automation) -> Unit,
    onRunNow: (Automation) -> Unit,
    onShowHistory: (Automation) -> Unit,
    onEditChain: (Automation) -> Unit,
    onEdit: (Automation) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    var draggedItemIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var dragOffset by rememberSaveable { mutableFloatStateOf(0f) }
    val isManualSort = sortOption == SortOption.MANUAL

    // The LazyList keeps the previously first visible item anchored by key across data
    // changes, so switching the sort order would scroll the list to follow that item
    // (e.g. from the top to where it ended up in the new order). Reset to the top
    // instead, so the new order always starts at the beginning.
    LaunchedEffect(sortOption) {
        if (items.isNotEmpty()) {
            lazyListState.scrollToItem(0)
        }
    }

    LazyColumn(
        state = lazyListState,
        contentPadding =
            PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 88.dp,
                top = 8.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
            Modifier
                .fillMaxSize()
                .then(
                    if (isManualSort) {
                        Modifier.pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    lazyListState.layoutInfo.visibleItemsInfo
                                        .find {
                                            it.offset <= offset.y.toInt() &&
                                                (it.offset + it.size) >= offset.y.toInt()
                                        }?.let { item ->
                                            draggedItemIndex = item.index
                                        }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount.y
                                    val currentIdx = draggedItemIndex ?: return@detectDragGesturesAfterLongPress
                                    val layoutInfo = lazyListState.layoutInfo
                                    val currentItem =
                                        layoutInfo.visibleItemsInfo.find { it.index == currentIdx }

                                    currentItem?.let {
                                        val targetCenter = it.offset + (it.size / 2) + dragOffset
                                        val targetItem =
                                            layoutInfo.visibleItemsInfo.find { info ->
                                                targetCenter.toInt() in info.offset..(info.offset + info.size)
                                            }

                                        if (targetItem != null && targetItem.index != currentIdx) {
                                            onMove(currentIdx, targetItem.index)
                                            draggedItemIndex = targetItem.index
                                            dragOffset = 0f
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggedItemIndex = null
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    draggedItemIndex = null
                                    dragOffset = 0f
                                },
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
    ) {
        itemsIndexed(items, key = { _, item -> item.automation.id }) { index, item ->
            val isDragging = isManualSort && index == draggedItemIndex

            val itemModifier =
                if (isDragging) {
                    Modifier
                        .zIndex(3f)
                        .graphicsLayer {
                            translationY = dragOffset
                            scaleX = 1.04f
                            scaleY = 1.04f
                            alpha = 0.9f
                            shadowElevation = 8.dp.toPx()
                        }
                } else {
                    Modifier
                }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(itemModifier),
            ) {
                AutomationItem(
                    item = item,
                    onToggle = { onToggle(item.automation.id, it) },
                    onDelete = { onDelete(item.automation) },
                    onRunNow = { onRunNow(item.automation) },
                    onShowHistory = { onShowHistory(item.automation) },
                    onEditChain = { onEditChain(item.automation) },
                    onEdit = { onEdit(item.automation) },
                )
            }
        }
    }
}

@Composable
private fun AutomationItem(
    item: AutomationUiItem,
    onToggle: (Boolean) -> Unit,
    onRunNow: () -> Unit,
    onDelete: () -> Unit,
    onShowHistory: () -> Unit,
    onEditChain: () -> Unit,
    onEdit: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .width(6.dp)
                        .background(Color(item.statusColor).copy(alpha = 0.8f)),
            )

            Column(
                modifier =
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    ScriptIcon(item.scriptIconPath, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.automation.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FrequencyBadge(type = item.automation.type)
                            if (item.automation.requireWifi) {
                                Icon(
                                    Icons.Default.Wifi,
                                    null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            if (item.automation.requireCharging) {
                                Icon(
                                    Icons.Default.BatteryChargingFull,
                                    null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                    Switch(
                        checked = item.automation.isEnabled,
                        onCheckedChange = onToggle,
                        modifier = Modifier.scale(0.8f),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = item.scriptName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onShowHistory() }
                                .padding(4.dp),
                    ) {
                        MonitoringInfo(
                            icon = Icons.Default.Event,
                            text = item.nextRunText,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        MonitoringInfo(
                            icon = if (item.automation.lastExitCode == 0) Icons.Default.CheckCircle else Icons.Default.Error,
                            text = item.lastRunText,
                            contentColor = Color(item.statusColor),
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledIconButton(
                            onClick = onRunNow,
                            modifier = Modifier.size(40.dp),
                            colors =
                                IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                ),
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                        }

                        Surface(
                            onClick = onEdit,
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.tertiary,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Edit,
                                    stringResource(R.string.cd_edit),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        Surface(
                            onClick = onEditChain,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Link,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        Surface(
                            onClick = onDelete,
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.DeleteSweep,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FrequencyBadge(type: AutomationType) {
    val text =
        when (type) {
            AutomationType.ONE_TIME -> stringResource(R.string.automation_type_one_time)
            AutomationType.PERIODIC -> stringResource(R.string.automation_type_periodic)
            AutomationType.WEEKLY -> stringResource(R.string.automation_type_weekly)
            AutomationType.BOOT -> stringResource(R.string.automation_type_boot)
            AutomationType.MONTHLY -> stringResource(R.string.automation_type_monthly)
            AutomationType.TIME_WINDOW -> stringResource(R.string.automation_type_time_window)
            AutomationType.RANDOM_DELAY -> stringResource(R.string.automation_type_random_delay)
            AutomationType.SCREEN_ON -> stringResource(R.string.automation_type_screen_on)
            AutomationType.SCREEN_OFF -> stringResource(R.string.automation_type_screen_off)
            AutomationType.NETWORK_CONNECTED -> stringResource(R.string.automation_type_network_connected)
            AutomationType.NETWORK_DISCONNECTED -> stringResource(R.string.automation_type_network_disconnected)
            AutomationType.USB_CONNECTED -> stringResource(R.string.automation_type_usb_connected)
            AutomationType.USB_DISCONNECTED -> stringResource(R.string.automation_type_usb_disconnected)
        }

    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MonitoringInfo(
    icon: ImageVector,
    text: String,
    contentColor: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = contentColor.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
        )
    }
}

@Composable
private fun PermissionWarningBanner(onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(16.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 0.dp, horizontal = 16.dp)
                .clickable { onClick() },
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.exact_alarm_permission_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@DevicePreviews
@Composable
fun AutomationScreenPreview() {
    MaterialTheme {
        AutomationScreen(
            uiState =
                AutomationUiState(
                    items = mockAutomations,
                    isExactAlarmPermissionGranted = true,
                ),
            sortOption = SortOption.MANUAL,
            onBackClick = {},
            onToggleAutomation = { _, _ -> },
            onDeleteAutomation = {},
            onAddAutomationClick = {},
            onRequestPermission = {},
            onRunNow = {},
            onShowHistory = {},
            onEditChain = {},
            onEditAutomation = {},
            onSortOptionChange = {},
            onMoveAutomation = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, name = "Automation Screen - Permission Missing")
@Composable
fun AutomationScreenPermissionPreview() {
    MaterialTheme {
        AutomationScreen(
            uiState =
                AutomationUiState(
                    items = mockAutomations.take(1),
                    isExactAlarmPermissionGranted = false,
                ),
            sortOption = SortOption.MANUAL,
            onBackClick = {},
            onToggleAutomation = { _, _ -> },
            onDeleteAutomation = {},
            onAddAutomationClick = {},
            onRequestPermission = {},
            onRunNow = {},
            onShowHistory = {},
            onEditChain = {},
            onEditAutomation = {},
            onSortOptionChange = {},
            onMoveAutomation = { _, _ -> },
        )
    }
}
