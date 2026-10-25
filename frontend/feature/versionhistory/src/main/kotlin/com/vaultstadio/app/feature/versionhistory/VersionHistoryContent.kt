package com.vaultstadio.app.feature.versionhistory

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun VersionHistoryContent(
    component: VersionHistoryComponent,
    modifier: Modifier = Modifier,
) {
    val viewModel: VersionHistoryViewModel = koinViewModel {
        parametersOf(component.itemId)
    }

    VersionHistoryScreen(
        itemId = component.itemId,
        itemName = component.itemName,
        versionHistory = viewModel.versionHistory,
        selectedVersion = viewModel.selectedVersion,
        versionDiff = viewModel.versionDiff,
        downloadUrl = viewModel.downloadUrl,
        isLoading = viewModel.isLoading,
        error = viewModel.error,
        onLoadHistory = viewModel::loadHistory,
        onGetVersion = viewModel::getVersion,
        onRestoreVersion = viewModel::restoreVersion,
        onDeleteVersion = viewModel::deleteVersion,
        onCompareVersions = viewModel::compareVersions,
        onDownloadVersion = viewModel::downloadVersion,
        onCleanupVersions = viewModel::cleanupVersions,
        onClearSelectedVersion = viewModel::clearSelectedVersion,
        onClearDownloadUrl = viewModel::clearDownloadUrl,
        onClearDiff = viewModel::clearDiff,
        onClearError = viewModel::clearError,
        onBack = component::onBack,
        modifier = modifier,
    )
}
