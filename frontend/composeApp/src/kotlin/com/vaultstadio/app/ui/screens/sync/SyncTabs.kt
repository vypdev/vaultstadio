/**
 * VaultStadio Sync Tabs
 *
 * Tab content for the Sync screen.
 */

package com.vaultstadio.app.ui.screens.sync

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.sync.model.SyncConflict
import com.vaultstadio.app.domain.sync.model.SyncDevice
import com.vaultstadio.app.ui.theme.VaultStadioPreview
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Tab content for devices.
 */
@Composable
fun DevicesTab(
    devices: List<SyncDevice>,
    isLoading: Boolean,
    isSyncing: Boolean,
    showSyncDialog: () -> Unit,
    showRegisterDialog: () -> Unit,
    onDeactivate: (SyncDevice) -> Unit,
    onRemove: (SyncDevice) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            devices.isEmpty() -> {
                EmptySyncState(modifier = Modifier.align(Alignment.Center))
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    DevicesActionBar(
                        isSyncing = isSyncing,
                        hasActiveDevices = devices.any { it.isActive },
                        onSyncClick = showSyncDialog,
                        onRegisterClick = showRegisterDialog,
                    )

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(devices) { device ->
                            DeviceCard(
                                device = device,
                                onDeactivate = { onDeactivate(device) },
                                onRemove = { onRemove(device) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DevicesActionBar(
    isSyncing: Boolean,
    hasActiveDevices: Boolean,
    onSyncClick: () -> Unit,
    onRegisterClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onSyncClick,
            modifier = Modifier.weight(1f),
            enabled = !isSyncing && hasActiveDevices,
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(8.dp))
                Text("Syncing...")
            } else {
                Icon(Icons.Default.Sync, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sync Now")
            }
        }
        OutlinedButton(onClick = onRegisterClick) {
            Icon(Icons.Default.Add, null, Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
            Text("Register")
        }
    }
}

/**
 * Tab content for conflicts.
 */
@Composable
fun ConflictsTab(
    conflicts: List<SyncConflict>,
    isLoading: Boolean,
    onResolve: (SyncConflict) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            conflicts.isEmpty() -> {
                NoConflictsState(modifier = Modifier.align(Alignment.Center))
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(conflicts) { conflict ->
                        ConflictCard(
                            conflict = conflict,
                            onResolve = { onResolve(conflict) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoConflictsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.Sync,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No Conflicts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "All your files are in sync",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// region Previews

@Preview
@Composable
internal fun DevicesTabEmptyPreview() {
    VaultStadioPreview {
        DevicesTab(
            devices = emptyList(),
            isLoading = false,
            isSyncing = false,
            showSyncDialog = {},
            showRegisterDialog = {},
            onDeactivate = {},
            onRemove = {},
        )
    }
}

@Preview
@Composable
internal fun ConflictsTabEmptyPreview() {
    VaultStadioPreview {
        ConflictsTab(
            conflicts = emptyList(),
            isLoading = false,
            onResolve = {},
        )
    }
}

// endregion
