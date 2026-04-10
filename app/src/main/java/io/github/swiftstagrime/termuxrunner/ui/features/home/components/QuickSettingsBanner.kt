package io.github.swiftstagrime.termuxrunner.ui.features.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.ui.preview.DevicePreviews
import java.io.File

@Composable
fun QuickSettingsBanner(
    tileMappings: Map<Int, Script?>,
    onTileClick: (Script) -> Unit,
    onEmptyTileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.title_tile_settings),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    val minItemWidth = 56.dp
                    val spacing = 8.dp
                    val totalMinWidth = (minItemWidth * 5) + (spacing * 4)
                    val canFitAll = maxWidth >= totalMinWidth

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .then(
                                    if (canFitAll) {
                                        Modifier
                                    } else {
                                        Modifier.horizontalScroll(rememberScrollState())
                                    },
                                ),
                        horizontalArrangement =
                            if (canFitAll) {
                                Arrangement.SpaceBetween
                            } else {
                                Arrangement.spacedBy(spacing)
                            },
                        verticalAlignment = Alignment.Top,
                    ) {
                        for (i in 1..5) {
                            TileCircleItem(
                                index = i,
                                script = tileMappings[i],
                                onClick = {
                                    val script = tileMappings[i]
                                    if (script != null) onTileClick(script) else onEmptyTileClick()
                                },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .padding(horizontal = 8.dp)
                                .width(1.dp)
                                .height(24.dp)
                                .background(MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.15f)),
                    )

                    IconButton(
                        onClick = onSettingsClick,
                        modifier =
                            Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.05f),
                                    CircleShape,
                                ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configure Tiles",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TileCircleItem(
    index: Int,
    script: Script?,
    onClick: () -> Unit,
) {
    val isAssigned = script != null

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(56.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClick)
                    .background(
                        if (isAssigned) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                        },
                    ).then(
                        if (!isAssigned) {
                            Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f),
                                CircleShape,
                            )
                        } else {
                            Modifier
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (script != null) {
                if (script.iconPath != null) {
                    AsyncImage(
                        model =
                            ImageRequest
                                .Builder(LocalContext.current)
                                .data(File(script.iconPath))
                                .crossfade(true)
                                .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = index.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                Icon(
                    Icons.Default.Add,
                    null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = script?.name ?: "",
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@DevicePreviews
@Composable
fun QuickSettingsBannerPreview() {
    val mockScripts =
        mapOf(
            1 to Script(id = 1, name = "Update System", code = ""),
            2 to Script(id = 2, name = "Clean Logs", code = ""),
            3 to null,
            4 to Script(id = 4, name = "Start Web", code = ""),
            5 to null,
        )

    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            QuickSettingsBanner(
                tileMappings = mockScripts,
                onTileClick = {},
                onEmptyTileClick = {},
                onSettingsClick = {},
            )
        }
    }
}
