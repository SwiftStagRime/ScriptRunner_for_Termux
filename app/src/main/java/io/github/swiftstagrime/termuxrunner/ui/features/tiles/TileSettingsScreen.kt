package io.github.swiftstagrime.termuxrunner.ui.features.tiles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.ui.preview.DevicePreviews
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TileSettingsScreen(
    tileMappings: Map<Int, Script?>,
    onBack: () -> Unit,
    onClearTile: (Int) -> Unit,
    onTileClicked: (Int) -> Unit,
) {
    val outerBackgroundColor = MaterialTheme.colorScheme.surface
    val sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest

    Scaffold(
        containerColor = outerBackgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.title_tile_settings),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
            )
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
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 1.dp,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(bottom = 4.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text =
                                    stringResource(
                                        R.string.assign_scripts_to_these_slots_you_can_then_trigger_them_directly_from_your_phone_s_quick_settings_panel,
                                    ),
                                style = MaterialTheme.typography.labelMedium,
                                lineHeight = TextUnit(16f, TextUnitType.Sp),
                            )
                        }
                    }
                }

                items(5) { index ->
                    val tileIndex = index + 1
                    val assignedScript = tileMappings[tileIndex]
                    val isAssigned = assignedScript != null

                    Card(
                        onClick = { onTileClicked(tileIndex) },
                        shape = RoundedCornerShape(20.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border =
                            BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            ),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isAssigned) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            },
                                        ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isAssigned && assignedScript.iconPath != null) {
                                    AsyncImage(
                                        model =
                                            ImageRequest
                                                .Builder(LocalContext.current)
                                                .data(File(assignedScript.iconPath))
                                                .crossfade(true)
                                                .build(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Text(
                                        text = tileIndex.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color =
                                            if (isAssigned) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.tile_slot_label, tileIndex),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = assignedScript?.name ?: stringResource(R.string.tile_unassigned),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isAssigned) FontWeight.Bold else FontWeight.Medium,
                                    color =
                                        if (isAssigned) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        },
                                )
                            }

                            if (isAssigned) {
                                IconButton(
                                    onClick = { onClearTile(tileIndex) },
                                    colors =
                                        IconButtonDefaults.iconButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error,
                                        ),
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.clear),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@DevicePreviews
@Composable
fun TileSettingsScreenPreview() {
    val mockMappings =
        mapOf(
            1 to Script(id = 1, name = "Production Backup", code = ""),
            2 to null,
            3 to Script(id = 3, name = "Check Network", code = ""),
            4 to null,
            5 to null,
        )

    MaterialTheme {
        TileSettingsScreen(
            tileMappings = mockMappings,
            onBack = {},
            onClearTile = {},
            onTileClicked = {},
        )
    }
}
