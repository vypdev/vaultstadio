/**
 * VaultStadio Federation Screen - Instances Tab
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.model.FederatedInstance
import com.vaultstadio.app.domain.model.FederationCapability
import com.vaultstadio.app.domain.model.InstanceStatus
import com.vaultstadio.app.ui.theme.VaultStadioPreview
import kotlinx.datetime.Clock
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Duration.Companion.days

private val SampleFederatedInstance = FederatedInstance(
    id = "instance-1",
    domain = "example.vaultstadio.com",
    name = "Example Instance",
    description = "A sample federated instance for testing purposes",
    version = "1.0.0",
    capabilities = listOf(
        FederationCapability.RECEIVE_SHARES,
        FederationCapability.SEND_SHARES,
        FederationCapability.FEDERATED_IDENTITY,
    ),
    status = InstanceStatus.ONLINE,
    lastSeenAt = Clock.System.now(),
    registeredAt = Clock.System.now().minus(30.days),
)

/**
 * Tab displaying federated instances.
 */
@Composable
fun InstancesTab(
    instances: List<FederatedInstance>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onBlock: (FederatedInstance) -> Unit,
    onRemove: (String) -> Unit,
    onViewDetails: (FederatedInstance) -> Unit,
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

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                instances.isEmpty() -> {
                    EmptyFederationState(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(instances) { instance ->
                            InstanceCard(
                                instance = instance,
                                onBlock = { onBlock(instance) },
                                onRemove = { onRemove(instance.id) },
                                onViewDetails = { onViewDetails(instance) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card displaying a federated instance.
 */
@Composable
fun InstanceCard(
    instance: FederatedInstance,
    onBlock: () -> Unit,
    onRemove: () -> Unit,
    onViewDetails: () -> Unit,
) {
    Card(
        onClick = onViewDetails,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (instance.status) {
                InstanceStatus.BLOCKED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                InstanceStatus.OFFLINE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when (instance.status) {
                            InstanceStatus.ONLINE -> Icons.Default.Cloud
                            InstanceStatus.BLOCKED -> Icons.Default.Block
                            else -> Icons.Default.CloudOff
                        },
                        contentDescription = null,
                        tint = when (instance.status) {
                            InstanceStatus.ONLINE -> Color(0xFF4CAF50)
                            InstanceStatus.BLOCKED -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            instance.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            instance.domain,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                StatusChip(status = instance.status)
            }

            instance.description?.let { desc ->
                Spacer(Modifier.height(8.dp))
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "v${instance.version}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row {
                    if (instance.status != InstanceStatus.BLOCKED) {
                        IconButton(onClick = onBlock) {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = "Block",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    IconButton(onClick = onRemove) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

// region Previews

@Preview
@Composable
internal fun InstancesTabPreview() {
    VaultStadioPreview {
        InstancesTab(
            instances = listOf(
                SampleFederatedInstance,
                SampleFederatedInstance.copy(
                    id = "instance-2",
                    domain = "another.vaultstadio.com",
                    name = "Another Instance",
                    status = InstanceStatus.PENDING,
                ),
            ),
            isLoading = false,
            onRefresh = {},
            onBlock = {},
            onRemove = {},
            onViewDetails = {},
        )
    }
}

@Preview
@Composable
internal fun InstancesTabLoadingPreview() {
    VaultStadioPreview {
        InstancesTab(
            instances = emptyList(),
            isLoading = true,
            onRefresh = {},
            onBlock = {},
            onRemove = {},
            onViewDetails = {},
        )
    }
}

@Preview
@Composable
internal fun InstancesTabEmptyPreview() {
    VaultStadioPreview {
        InstancesTab(
            instances = emptyList(),
            isLoading = false,
            onRefresh = {},
            onBlock = {},
            onRemove = {},
            onViewDetails = {},
        )
    }
}

@Preview
@Composable
internal fun InstanceCardPreview() {
    VaultStadioPreview {
        InstanceCard(
            instance = SampleFederatedInstance,
            onBlock = {},
            onRemove = {},
            onViewDetails = {},
        )
    }
}

@Preview
@Composable
internal fun InstanceCardBlockedPreview() {
    VaultStadioPreview {
        InstanceCard(
            instance = SampleFederatedInstance.copy(status = InstanceStatus.BLOCKED),
            onBlock = {},
            onRemove = {},
            onViewDetails = {},
        )
    }
}

// endregion
