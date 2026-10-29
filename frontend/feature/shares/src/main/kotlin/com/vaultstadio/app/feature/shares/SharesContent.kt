package com.vaultstadio.app.feature.shares

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SharesContent(
    @Suppress("UNUSED_PARAMETER") component: SharesComponent,
    modifier: Modifier = Modifier,
) {
    val viewModel: SharesViewModel = koinViewModel()

    DisposableEffect(viewModel) { onDispose { viewModel.cancelCopy() } }

    SharedScreen(
        shares = viewModel.shares,
        isLoading = viewModel.isLoading,
        error = viewModel.error,
        clipboardLink = viewModel.clipboardLink,
        isCopyingLink = viewModel.isCopyingLink,
        copyError = viewModel.copyError,
        onClearCopyError = viewModel::clearCopyError,
        onCopyLink = viewModel::copyLink,
        onDeleteShare = viewModel::deleteShare,
        onRefresh = viewModel::loadShares,
        onClearClipboardLink = viewModel::clearClipboardLink,
        onClearError = viewModel::clearError,
        modifier = modifier,
    )
}
