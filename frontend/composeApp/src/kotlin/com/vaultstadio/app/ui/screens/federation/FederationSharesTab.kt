/**
 * VaultStadio Federation Screen - Shares Tab
 */

package com.vaultstadio.app.ui.screens.federation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.model.FederatedShare
import com.vaultstadio.app.domain.model.FederatedShareStatus
import com.vaultstadio.app.domain.model.SharePermission
import com.vaultstadio.app.ui.theme.VaultStadioPreview
import kotlin.time.Clock
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Duration.Companion.days

private val SampleFederatedShare = FederatedShare(
    id = "share-1",
    itemId = "item-123",
    sourceInstance = "source.vaultstadio.com",
    targetInstance = "target.vaultstadio.com",
    targetUserId = "user-456",
    permissions = listOf(SharePermission.READ, SharePermission.WRITE),
    status = FederatedShareStatus.PENDING,
    expiresAt = Clock.System.now().plus(30.days),
    createdBy = "user-1",
    createdAt = Clock.System.now().minus(1.days),
    acceptedAt = null,
)

/**
 * Tab displaying federated shares.
 */
@Composable
fun SharesTab(
    outgoingShares: List<FederatedShare>,
    incomingShares: List<FederatedShare>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
    onRevoke: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Refresh button row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                outgoingShares.isEmpty() && incomingShares.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Default.Share,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("No Federated Shares", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Share files with other VaultStadio instances",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (incomingShares.isNotEmpty()) {
                            item {
                                Text(
                                    "Incoming Shares",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                            items(incomingShares) { share ->
                                FederatedShareCard(
                                    share = share,
                                    isIncoming = true,
                                    onAccept = { onAccept(share.id) },
                                    onDecline = { onDecline(share.id) },
                                    onRevoke = null,
                                )
                            }
                        }
                        if (outgoingShares.isNotEmpty()) {
                            item {
                                Text(
                                    "Outgoing Shares",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                            items(outgoingShares) { share ->
                                FederatedShareCard(
                                    share = share,
                                    isIncoming = false,
                                    onAccept = null,
                                    onDecline = null,
                                    onRevoke = { onRevoke(share.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card displaying a federated share.
 */
@Composable
fun FederatedShareCard(
    share: FederatedShare,
    isIncoming: Boolean,
    onAccept: (() -> Unit)?,
    onDecline: (() -> Unit)?,
    onRevoke: (() -> Unit)?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isIncoming) "From: ${share.sourceInstance}" else "To: ${share.targetInstance}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "Item: ${share.itemId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (share.status) {
                        FederatedShareStatus.ACCEPTED -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                        FederatedShareStatus.PENDING -> Color(0xFFFFC107).copy(alpha = 0.15f)
                        FederatedShareStatus.DECLINED, FederatedShareStatus.REVOKED ->
                            MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                ) {
                    Text(
                        share.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "Permissions: ${share.permissions.joinToString(", ") { it.name }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (share.status == FederatedShareStatus.PENDING && isIncoming) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onAccept?.invoke() }) {
                        Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Accept")
                    }
                    OutlinedButton(onClick = { onDecline?.invoke() }) {
                        Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Decline")
                    }
                }
            }

            if (share.status == FederatedShareStatus.ACCEPTED && !isIncoming) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { onRevoke?.invoke() }) {
                    Text("Revoke")
                }
            }
        }
    }
}

// region Previews

@Preview
@Composable
internal fun SharesTabPreview() {
    VaultStadioPreview {
        SharesTab(
            outgoingShares = listOf(
                SampleFederatedShare.copy(
                    id = "share-out-1",
                    status = FederatedShareStatus.ACCEPTED,
                ),
            ),
            incomingShares = listOf(SampleFederatedShare),
            isLoading = false,
            onRefresh = {},
            onAccept = {},
            onDecline = {},
            onRevoke = {},
        )
    }
}

@Preview
@Composable
internal fun SharesTabLoadingPreview() {
    VaultStadioPreview {
        SharesTab(
            outgoingShares = emptyList(),
            incomingShares = emptyList(),
            isLoading = true,
            onRefresh = {},
            onAccept = {},
            onDecline = {},
            onRevoke = {},
        )
    }
}

@Preview
@Composable
internal fun SharesTabEmptyPreview() {
    VaultStadioPreview {
        SharesTab(
            outgoingShares = emptyList(),
            incomingShares = emptyList(),
            isLoading = false,
            onRefresh = {},
            onAccept = {},
            onDecline = {},
            onRevoke = {},
        )
    }
}

@Preview
@Composable
internal fun FederatedShareCardIncomingPreview() {
    VaultStadioPreview {
        FederatedShareCard(
            share = SampleFederatedShare,
            isIncoming = true,
            onAccept = {},
            onDecline = {},
            onRevoke = null,
        )
    }
}

@Preview
@Composable
internal fun FederatedShareCardOutgoingPreview() {
    VaultStadioPreview {
        FederatedShareCard(
            share = SampleFederatedShare.copy(
                status = FederatedShareStatus.ACCEPTED,
            ),
            isIncoming = false,
            onAccept = null,
            onDecline = null,
            onRevoke = {},
        )
    }
}

// endregion
