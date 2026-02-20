package com.vaultstadio.app.feature.sync

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SyncContent(
    @Suppress("UNUSED_PARAMETER") component: SyncComponent,
    modifier: Modifier = Modifier,
) {
    val viewModel: SyncViewModel = koinViewModel()

    SyncScreen(
        devices = viewModel.devices,
        conflicts = viewModel.conflicts,
        syncResponse = viewModel.syncResponse,
        isLoading = viewModel.isLoading,
        isSyncing = viewModel.isSyncing,
        error = viewModel.error,
        onLoadDevices = viewModel::loadDevices,
        onLoadConflicts = viewModel::loadConflicts,
        onPullChanges = viewModel::pullChanges,
        onRegisterDevice = { deviceId, deviceName, deviceType ->
            viewModel.registerDevice(deviceId, deviceName, deviceType)
        },
        onDeactivateDevice = viewModel::deactivateDevice,
        onRemoveDevice = viewModel::removeDevice,
        onResolveConflict = viewModel::resolveConflict,
        onClearSyncResponse = viewModel::clearSyncResponse,
        onClearError = viewModel::clearError,
        modifier = modifier,
    )
}
