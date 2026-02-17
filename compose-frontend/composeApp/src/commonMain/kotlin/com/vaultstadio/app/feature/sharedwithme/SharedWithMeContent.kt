package com.vaultstadio.app.feature.sharedwithme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vaultstadio.app.ui.screens.SharedWithMeScreen
import org.koin.compose.viewmodel.koinViewModel

/**
 * SharedWithMe feature content - delegates to SharedWithMeScreen with ViewModel data.
 */
@Composable
fun SharedWithMeContent(
    component: SharedWithMeComponent,
    modifier: Modifier = Modifier,
) {
    val viewModel: SharedWithMeViewModel = koinViewModel()

    SharedWithMeScreen(
        sharedItems = viewModel.sharedItems,
        selectedItem = viewModel.selectedItem,
        downloadUrl = viewModel.downloadUrl,
        isLoading = viewModel.isLoading,
        error = viewModel.error,
        onLoadSharedItems = viewModel::loadSharedItems,
        onItemClick = viewModel::onItemClick,
        onDownload = viewModel::downloadItem,
        onRemoveShare = viewModel::removeShare,
        onClearSelectedItem = viewModel::clearSelectedItem,
        onClearDownloadUrl = viewModel::clearDownloadUrl,
        onClearError = viewModel::clearError,
        onBack = component::onBack,
        modifier = modifier,
    )
}
