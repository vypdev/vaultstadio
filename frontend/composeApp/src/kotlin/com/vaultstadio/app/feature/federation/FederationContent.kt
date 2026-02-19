package com.vaultstadio.app.feature.federation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vaultstadio.app.ui.screens.FederationScreen
import org.koin.compose.viewmodel.koinViewModel

/**
 * Federation feature content - delegates to FederationScreen with ViewModel data.
 */
@Composable
fun FederationContent(
    @Suppress("UNUSED_PARAMETER") component: FederationComponent,
    modifier: Modifier = Modifier,
) {
    val viewModel: FederationViewModel = koinViewModel()

    FederationScreen(
        instances = viewModel.instances,
        outgoingShares = viewModel.outgoingShares,
        incomingShares = viewModel.incomingShares,
        identities = viewModel.identities,
        activities = viewModel.activities,
        selectedInstance = viewModel.selectedInstance,
        isLoading = viewModel.isLoading,
        error = viewModel.error,
        onLoadInstances = viewModel::loadInstances,
        onLoadShares = viewModel::loadShares,
        onLoadIdentities = viewModel::loadIdentities,
        onLoadActivities = viewModel::loadActivities,
        onGetInstanceDetails = viewModel::getInstanceDetails,
        onRequestFederation = viewModel::requestFederation,
        onBlockInstance = viewModel::blockInstance,
        onRemoveInstance = viewModel::removeInstance,
        onAcceptShare = viewModel::acceptShare,
        onDeclineShare = viewModel::declineShare,
        onRevokeShare = viewModel::revokeShare,
        onLinkIdentity = viewModel::linkIdentity,
        onUnlinkIdentity = viewModel::unlinkIdentity,
        onClearSelectedInstance = viewModel::clearSelectedInstance,
        onClearError = viewModel::clearError,
        modifier = modifier,
    )
}
