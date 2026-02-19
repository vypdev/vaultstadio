/**
 * VaultStadio Federation Screen
 *
 * Screen for managing federated instances and shares.
 * Refactored to use modular components from the federation package.
 */

package com.vaultstadio.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import com.vaultstadio.app.domain.model.FederatedActivity
import com.vaultstadio.app.domain.model.FederatedIdentity
import com.vaultstadio.app.domain.model.FederatedInstance
import com.vaultstadio.app.domain.model.FederatedShare
import com.vaultstadio.app.domain.model.FederatedShareStatus
import com.vaultstadio.app.i18n.strings
import com.vaultstadio.app.ui.screens.federation.ActivitiesTab
import com.vaultstadio.app.ui.screens.federation.BlockInstanceDialog
import com.vaultstadio.app.ui.screens.federation.ErrorDialog
import com.vaultstadio.app.ui.screens.federation.IdentitiesTab
import com.vaultstadio.app.ui.screens.federation.InstanceDetailsDialog
import com.vaultstadio.app.ui.screens.federation.InstancesTab
import com.vaultstadio.app.ui.screens.federation.LinkIdentityDialog
import com.vaultstadio.app.ui.screens.federation.RequestFederationDialog
import com.vaultstadio.app.ui.screens.federation.SharesTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FederationScreen(
    instances: List<FederatedInstance>,
    outgoingShares: List<FederatedShare>,
    incomingShares: List<FederatedShare>,
    identities: List<FederatedIdentity>,
    activities: List<FederatedActivity>,
    selectedInstance: FederatedInstance?,
    isLoading: Boolean,
    error: String?,
    onLoadInstances: () -> Unit,
    onLoadShares: () -> Unit,
    onLoadIdentities: () -> Unit,
    onLoadActivities: () -> Unit,
    onGetInstanceDetails: (String) -> Unit,
    onRequestFederation: (String, String?) -> Unit,
    onBlockInstance: (String) -> Unit,
    onRemoveInstance: (String) -> Unit,
    onAcceptShare: (String) -> Unit,
    onDeclineShare: (String) -> Unit,
    onRevokeShare: (String) -> Unit,
    onLinkIdentity: (String, String, String) -> Unit,
    onUnlinkIdentity: (String) -> Unit,
    onClearSelectedInstance: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = strings()
    var selectedTab by remember { mutableStateOf(0) }
    var showRequestDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf<FederatedInstance?>(null) }
    var showLinkIdentityDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onLoadInstances()
        onLoadShares()
        onLoadIdentities()
        onLoadActivities()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(strings.federationTitle) })
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { showRequestDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Request Federation")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(strings.federationInstances) },
                    icon = { Icon(Icons.Default.Hub, null, Modifier.size(18.dp)) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { SharesTabLabel(incomingShares, strings.federationShares) },
                    icon = { Icon(Icons.Default.Share, null, Modifier.size(18.dp)) },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(strings.federationIdentities) },
                    icon = { Icon(Icons.Default.People, null, Modifier.size(18.dp)) },
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Activity") },
                    icon = { Icon(Icons.Default.Timeline, null, Modifier.size(18.dp)) },
                )
            }

            when (selectedTab) {
                0 -> InstancesTab(
                    instances = instances,
                    isLoading = isLoading,
                    onRefresh = onLoadInstances,
                    onBlock = { showBlockDialog = it },
                    onRemove = onRemoveInstance,
                    onViewDetails = { instance -> onGetInstanceDetails(instance.id) },
                )
                1 -> SharesTab(
                    outgoingShares = outgoingShares,
                    incomingShares = incomingShares,
                    isLoading = isLoading,
                    onRefresh = onLoadShares,
                    onAccept = onAcceptShare,
                    onDecline = onDeclineShare,
                    onRevoke = onRevokeShare,
                )
                2 -> IdentitiesTab(
                    identities = identities,
                    isLoading = isLoading,
                    onRefresh = onLoadIdentities,
                    onUnlink = onUnlinkIdentity,
                    onLinkIdentity = { showLinkIdentityDialog = true },
                )
                3 -> ActivitiesTab(
                    activities = activities,
                    isLoading = isLoading,
                    onRefresh = onLoadActivities,
                )
            }
        }
    }

    // Dialogs
    if (showRequestDialog) {
        RequestFederationDialog(
            strings = strings,
            onDismiss = { showRequestDialog = false },
            onConfirm = { domain, message ->
                onRequestFederation(domain, message)
                showRequestDialog = false
            },
        )
    }

    showBlockDialog?.let { instance ->
        BlockInstanceDialog(
            instance = instance,
            strings = strings,
            onDismiss = { showBlockDialog = null },
            onConfirm = {
                onBlockInstance(instance.id)
                showBlockDialog = null
            },
        )
    }

    selectedInstance?.let { instance ->
        InstanceDetailsDialog(
            instance = instance,
            onDismiss = onClearSelectedInstance,
        )
    }

    if (showLinkIdentityDialog) {
        LinkIdentityDialog(
            strings = strings,
            onDismiss = { showLinkIdentityDialog = false },
            onConfirm = { remoteUserId, remoteInstance, displayName ->
                onLinkIdentity(remoteUserId, remoteInstance, displayName)
                showLinkIdentityDialog = false
            },
        )
    }

    error?.let { errorMessage ->
        ErrorDialog(
            errorMessage = errorMessage,
            onDismiss = onClearError,
        )
    }
}

@Composable
private fun SharesTabLabel(incomingShares: List<FederatedShare>, label: String) {
    val pendingCount = incomingShares.count { it.status == FederatedShareStatus.PENDING }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        if (pendingCount > 0) {
            Spacer(Modifier.width(4.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary,
            ) {
                Text(
                    "$pendingCount",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}
