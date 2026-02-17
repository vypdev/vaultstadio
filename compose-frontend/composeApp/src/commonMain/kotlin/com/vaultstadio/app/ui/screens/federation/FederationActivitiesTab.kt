/**
 * VaultStadio Federation Screen - Activities Tab
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.model.FederatedActivity
import com.vaultstadio.app.domain.model.FederatedActivityType
import com.vaultstadio.app.ui.theme.VaultStadioPreview
import com.vaultstadio.app.utils.formatRelativeTime
import kotlinx.datetime.Clock
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

private val SampleFederatedActivity = FederatedActivity(
    id = "activity-1",
    instanceDomain = "example.vaultstadio.com",
    activityType = FederatedActivityType.SHARE_CREATED,
    actorId = "user-123@example.vaultstadio.com",
    objectId = "item-456",
    objectType = "file",
    summary = "File 'document.pdf' was shared",
    timestamp = Clock.System.now().minus(2.hours),
)

/**
 * Tab displaying federation activities.
 */
@Composable
fun ActivitiesTab(
    activities: List<FederatedActivity>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
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
                activities.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Default.Timeline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No federation activity",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(activities) { activity ->
                            ActivityCard(activity = activity)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card displaying a federation activity.
 */
@Composable
fun ActivityCard(activity: FederatedActivity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Timeline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    activity.activityType.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    activity.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${activity.instanceDomain} • ${formatRelativeTime(activity.timestamp)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// region Previews

@Preview
@Composable
internal fun ActivitiesTabPreview() {
    VaultStadioPreview {
        ActivitiesTab(
            activities = listOf(
                SampleFederatedActivity,
                SampleFederatedActivity.copy(
                    id = "activity-2",
                    activityType = FederatedActivityType.FILE_ACCESSED,
                    summary = "File 'presentation.pptx' was accessed",
                    timestamp = Clock.System.now().minus(5.hours),
                ),
                SampleFederatedActivity.copy(
                    id = "activity-3",
                    activityType = FederatedActivityType.INSTANCE_ONLINE,
                    summary = "Instance came online",
                    timestamp = Clock.System.now().minus(1.days),
                ),
            ),
            isLoading = false,
            onRefresh = {},
        )
    }
}

@Preview
@Composable
internal fun ActivitiesTabLoadingPreview() {
    VaultStadioPreview {
        ActivitiesTab(
            activities = emptyList(),
            isLoading = true,
            onRefresh = {},
        )
    }
}

@Preview
@Composable
internal fun ActivitiesTabEmptyPreview() {
    VaultStadioPreview {
        ActivitiesTab(
            activities = emptyList(),
            isLoading = false,
            onRefresh = {},
        )
    }
}

@Preview
@Composable
internal fun ActivityCardPreview() {
    VaultStadioPreview {
        ActivityCard(activity = SampleFederatedActivity)
    }
}

// endregion
