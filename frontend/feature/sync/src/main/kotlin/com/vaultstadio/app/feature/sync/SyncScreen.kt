package com.vaultstadio.app.feature.sync

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.core.resources.StringResources
import com.vaultstadio.app.core.resources.strings
import com.vaultstadio.app.domain.sync.model.ConflictResolution
import com.vaultstadio.app.domain.sync.model.SyncConflict
import com.vaultstadio.app.domain.sync.model.SyncDevice
import com.vaultstadio.app.domain.sync.model.SyncResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    devices: List<SyncDevice>,
    conflicts: List<SyncConflict>,
    syncResponse: SyncResponse?,
    isLoading: Boolean,
    isSyncing: Boolean,
    error: String?,
    onLoadDevices: () -> Unit,
    onLoadConflicts: () -> Unit,
    onPullChanges: (String, String?) -> Unit,
    onRegisterDevice: (String, String, String) -> Unit,
    onDeactivateDevice: (String) -> Unit,
    onRemoveDevice: (String) -> Unit,
    onResolveConflict: (String, ConflictResolution) -> Unit,
    onClearSyncResponse: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = strings()
    var selectedTab by remember { mutableStateOf(0) }
    var showDeactivateDialog by remember { mutableStateOf<SyncDevice?>(null) }
    var showRemoveDialog by remember { mutableStateOf<SyncDevice?>(null) }
    var showConflictDialog by remember { mutableStateOf<SyncConflict?>(null) }
    var showSyncDialog by remember { mutableStateOf(false) }
    var showRegisterDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onLoadDevices()
        onLoadConflicts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.syncTitle) },
                actions = {
                    if (conflicts.isNotEmpty()) {
                        ConflictsBadge(count = conflicts.size)
                        Spacer(Modifier.width(8.dp))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SyncTabRow(
                selectedTab = selectedTab,
                conflictsCount = conflicts.size,
                onTabSelected = { selectedTab = it },
                strings = strings,
            )

            when (selectedTab) {
                0 -> DevicesTab(
                    devices = devices,
                    isLoading = isLoading,
                    isSyncing = isSyncing,
                    showSyncDialog = { showSyncDialog = true },
                    showRegisterDialog = { showRegisterDialog = true },
                    onDeactivate = { showDeactivateDialog = it },
                    onRemove = { showRemoveDialog = it },
                )
                1 -> ConflictsTab(
                    conflicts = conflicts,
                    isLoading = isLoading,
                    onResolve = { showConflictDialog = it },
                )
            }
        }
    }

    showDeactivateDialog?.let { device ->
        DeactivateDeviceDialog(
            onConfirm = {
                onDeactivateDevice(device.deviceId)
                showDeactivateDialog = null
            },
            onDismiss = { showDeactivateDialog = null },
            strings = strings,
        )
    }

    showRemoveDialog?.let { device ->
        RemoveDeviceDialog(
            onConfirm = {
                onRemoveDevice(device.deviceId)
                showRemoveDialog = null
            },
            onDismiss = { showRemoveDialog = null },
            strings = strings,
        )
    }

    showConflictDialog?.let { conflict ->
        ConflictResolutionDialog(
            conflict = conflict,
            onResolve = { resolution ->
                onResolveConflict(conflict.id, resolution)
                showConflictDialog = null
            },
            onDismiss = { showConflictDialog = null },
            strings = strings,
        )
    }

    if (showSyncDialog) {
        SyncDeviceDialog(
            devices = devices,
            isSyncing = isSyncing,
            onSync = { device ->
                onPullChanges(device.id, null)
                showSyncDialog = false
            },
            onDismiss = { showSyncDialog = false },
            strings = strings,
        )
    }

    syncResponse?.let { response ->
        SyncResponseDialog(
            response = response,
            onDismiss = onClearSyncResponse,
        )
    }

    if (showRegisterDialog) {
        RegisterDeviceDialog(
            onRegister = { deviceId, deviceName, deviceType ->
                onRegisterDevice(deviceId, deviceName, deviceType)
                showRegisterDialog = false
            },
            onDismiss = { showRegisterDialog = false },
            strings = strings,
        )
    }

    error?.let { errorMessage ->
        SyncErrorDialog(
            message = errorMessage,
            onDismiss = onClearError,
        )
    }
}

@Composable
private fun ConflictsBadge(count: Int) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Warning,
                null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "$count conflict${if (count > 1) "s" else ""}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun SyncTabRow(
    selectedTab: Int,
    conflictsCount: Int,
    onTabSelected: (Int) -> Unit,
    strings: StringResources,
) {
    TabRow(selectedTabIndex = selectedTab) {
        Tab(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            text = { Text(strings.syncDevices) },
            icon = { Icon(Icons.Default.Computer, null) },
        )
        Tab(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.syncConflicts)
                    if (conflictsCount > 0) {
                        Spacer(Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.error,
                        ) {
                            Text(
                                "$conflictsCount",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onError,
                            )
                        }
                    }
                }
            },
            icon = { Icon(Icons.Default.SyncProblem, null) },
        )
    }
}
