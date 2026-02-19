/**
 * VaultStadio Sync Dialogs
 *
 * Dialog components for the Sync screen.
 */

package com.vaultstadio.app.ui.screens.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.sync.model.ConflictResolution
import com.vaultstadio.app.domain.sync.model.SyncConflict
import com.vaultstadio.app.domain.sync.model.SyncDevice
import com.vaultstadio.app.domain.sync.model.SyncResponse
import com.vaultstadio.app.i18n.StringResources
import com.vaultstadio.app.i18n.Strings
import com.vaultstadio.app.ui.theme.VaultStadioPreview
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Dialog for deactivating a device.
 */
@Composable
fun DeactivateDeviceDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    strings: StringResources,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.syncDeactivate) },
        text = { Text(strings.syncDeactivateConfirm) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(strings.commonDeactivate)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.actionCancel)
            }
        },
    )
}

/**
 * Dialog for removing a device.
 */
@Composable
fun RemoveDeviceDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    strings: StringResources,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.syncRemove) },
        text = { Text(strings.syncRemoveConfirm) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(strings.commonRemove)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.actionCancel)
            }
        },
    )
}

/**
 * Dialog for resolving a sync conflict.
 */
@Composable
fun ConflictResolutionDialog(
    conflict: SyncConflict,
    onResolve: (ConflictResolution) -> Unit,
    onDismiss: () -> Unit,
    strings: StringResources,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.syncResolveConflict) },
        text = {
            Column {
                Text(strings.syncChooseResolution)
                Spacer(Modifier.height(16.dp))

                ConflictResolution.entries.forEach { resolution ->
                    OutlinedButton(
                        onClick = { onResolve(resolution) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            when (resolution) {
                                ConflictResolution.KEEP_LOCAL -> strings.syncKeepLocalVersion
                                ConflictResolution.KEEP_REMOTE -> strings.syncKeepRemoteVersion
                                ConflictResolution.KEEP_BOTH -> strings.syncKeepBothRename
                                ConflictResolution.MERGE -> strings.syncMergeChanges
                                ConflictResolution.MANUAL -> strings.syncResolveManually
                            },
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.actionCancel)
            }
        },
    )
}

/**
 * Dialog for selecting a device to sync.
 */
@Composable
fun SyncDeviceDialog(
    devices: List<SyncDevice>,
    isSyncing: Boolean,
    onSync: (SyncDevice) -> Unit,
    onDismiss: () -> Unit,
    strings: StringResources,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sync Now") },
        text = {
            Column {
                Text("Select a device to sync:")
                Spacer(Modifier.height(16.dp))

                val activeDevices = devices.filter { it.isActive }
                if (activeDevices.isEmpty()) {
                    Text(
                        "No active devices. Register a device first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    activeDevices.forEach { device ->
                        OutlinedButton(
                            onClick = { onSync(device) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSyncing,
                        ) {
                            Icon(
                                getDeviceIcon(device.deviceType),
                                null,
                                Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(device.deviceName)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                if (isSyncing) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Syncing...")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.actionCancel)
            }
        },
    )
}

/**
 * Dialog showing sync response.
 */
@Composable
fun SyncResponseDialog(
    response: SyncResponse,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sync Complete") },
        text = {
            Column {
                Text("Changes pulled: ${response.changes.size}")
                if (response.hasMore) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "More changes available. Sync again to get them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                response.cursor?.let { cursor ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Cursor: ${cursor.take(20)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
    )
}

/**
 * Dialog for registering a new device.
 */
@Composable
fun RegisterDeviceDialog(
    onRegister: (String, String, String) -> Unit,
    onDismiss: () -> Unit,
    strings: StringResources,
) {
    var deviceName by remember { mutableStateOf("") }
    var deviceType by remember { mutableStateOf("DESKTOP") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register This Device") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Register this device to sync your files across all your devices.")
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Device Name") },
                    placeholder = { Text("My Computer") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = deviceType == "DESKTOP",
                        onClick = { deviceType = "DESKTOP" },
                        label = { Text("Desktop") },
                    )
                    FilterChip(
                        selected = deviceType == "MOBILE",
                        onClick = { deviceType = "MOBILE" },
                        label = { Text("Mobile") },
                    )
                    FilterChip(
                        selected = deviceType == "TABLET",
                        onClick = { deviceType = "TABLET" },
                        label = { Text("Tablet") },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (deviceName.isNotBlank()) {
                        val deviceId = "device-${kotlin.random.Random.nextLong()}"
                        onRegister(deviceId, deviceName, deviceType)
                    }
                },
                enabled = deviceName.isNotBlank(),
            ) {
                Text("Register Device")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.actionCancel)
            }
        },
    )
}

/**
 * Generic error dialog.
 */
@Composable
fun SyncErrorDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
    )
}

// region Previews

@Preview
@Composable
internal fun DeactivateDeviceDialogPreview() {
    VaultStadioPreview {
        DeactivateDeviceDialog(
            onConfirm = {},
            onDismiss = {},
            strings = Strings.resources,
        )
    }
}

@Preview
@Composable
internal fun SyncErrorDialogPreview() {
    VaultStadioPreview {
        SyncErrorDialog(
            message = "Failed to sync device. Please check your connection.",
            onDismiss = {},
        )
    }
}

// endregion
