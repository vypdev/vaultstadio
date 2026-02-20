package com.vaultstadio.app.feature.shares

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SharesContent(
    @Suppress("UNUSED_PARAMETER") component: SharesComponent,
    modifier: Modifier = Modifier,
) {
    val viewModel: SharesViewModel = koinViewModel()

    SharedScreen(
        shares = viewModel.shares,
        isLoading = viewModel.isLoading,
        error = viewModel.error,
        clipboardLink = viewModel.clipboardLink,
        onCopyLink = viewModel::copyLink,
        onDeleteShare = viewModel::deleteShare,
        onRefresh = viewModel::loadShares,
        onClearClipboardLink = viewModel::clearClipboardLink,
        onClearError = viewModel::clearError,
        modifier = modifier,
    )
}
