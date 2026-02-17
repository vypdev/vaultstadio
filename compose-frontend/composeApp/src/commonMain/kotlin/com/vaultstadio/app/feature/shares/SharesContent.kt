package com.vaultstadio.app.feature.shares

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vaultstadio.app.ui.screens.SharedScreen
import org.koin.compose.viewmodel.koinViewModel

/**
 * Shares feature content - delegates to SharedScreen with ViewModel data.
 */
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
