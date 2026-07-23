package io.github.swiftstagrime.termuxrunner.ui.features.scriptversions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.swiftstagrime.termuxrunner.domain.model.ScriptVersion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptVersionsScreen(
    onBack: () -> Unit,
    viewModel: ScriptVersionsViewModel,
) {
    val versions by viewModel.versions.collectAsStateWithLifecycle()

    var versionToDelete by remember { mutableStateOf<ScriptVersion?>(null) }
    var versionToRestore by remember { mutableStateOf<ScriptVersion?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("Code Versions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Surface(
            modifier =
                Modifier
                    .padding(padding)
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 1.dp,
        ) {
            if (versions.isEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No versions saved yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding =
                        androidx.compose.foundation.layout
                            .PaddingValues(vertical = 8.dp),
                ) {
                    items(versions, key = { it.id }) { version ->
                        VersionCard(
                            version = version,
                            onRestore = { versionToRestore = version },
                            onDelete = { versionToDelete = version },
                        )
                    }
                }
            }
        }
    }

    versionToRestore?.let { version ->
        AlertDialog(
            onDismissRequest = { versionToRestore = null },
            title = { Text("Restore Version") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(version.label ?: "Version")
                    Text(
                        "Current code will be saved as a new version before restoring.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.restoreVersion(version.id)
                    versionToRestore = null
                }) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { versionToRestore = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    versionToDelete?.let { version ->
        AlertDialog(
            onDismissRequest = { versionToDelete = null },
            title = { Text("Delete Version") },
            text = {
                Text(
                    "Are you sure you want to delete this version? This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteVersion(version)
                    versionToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { versionToDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun VersionCard(
    version: ScriptVersion,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clickable { onRestore() },
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .padding(14.dp)
                    .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Icon(
                    Icons.Default.Code,
                    null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    version.label ?: "Version",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    ScriptVersionsViewModel.formatTimestamp(version.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(onClick = onRestore) {
                Icon(Icons.Default.Restore, "Restore", tint = MaterialTheme.colorScheme.primary)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
