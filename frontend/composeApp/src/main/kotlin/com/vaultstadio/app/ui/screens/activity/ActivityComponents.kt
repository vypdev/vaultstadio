/**
 * VaultStadio Activity Components
 *
 * Reusable UI components for the Activity screen.
 */

package com.vaultstadio.app.ui.screens.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.activity.model.Activity
import com.vaultstadio.app.ui.theme.VaultStadioPreview
import com.vaultstadio.app.utils.formatRelativeTime
import kotlin.time.Clock
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

private val SampleActivity = Activity(
    id = "activity-1",
    type = "upload",
    userId = "user-1",
    itemId = "item-1",
    itemPath = "/Documents/example.pdf",
    details = "File uploaded successfully",
    createdAt = Clock.System.now().minus(1.hours),
)

private val SampleActivityDelete = Activity(
    id = "activity-2",
    type = "delete",
    userId = "user-1",
    itemId = "item-2",
    itemPath = "/Photos/vacation.jpg",
    details = null,
    createdAt = Clock.System.now().minus(1.days),
)

private val SampleActivityLogin = Activity(
    id = "activity-3",
    type = "login",
    userId = "user-1",
    itemId = null,
    itemPath = null,
    details = "Successful login from Chrome on macOS",
    createdAt = Clock.System.now().minus(2.hours),
)

/**
 * Card displaying an activity entry.
 */
@Composable
fun ActivityCard(
    activity: Activity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (icon, color) = getActivityIconAndColor(activity.type)

    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = color,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    getActivityTitle(activity.type),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                activity.itemPath?.let { path ->
                    Text(
                        path,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Text(
                formatRelativeTime(activity.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Empty state when no activities exist.
 */
@Composable
fun EmptyActivityState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.Timeline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No Activity",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Your recent activity will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Activity detail dialog.
 */
@Composable
fun ActivityDetailDialog(
    activity: Activity,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(getActivityTitle(activity.type)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Type: ${activity.type}")
                Text("Time: ${formatRelativeTime(activity.createdAt)}")
                activity.itemPath?.let { path -> Text("Path: $path") }
                activity.itemId?.let { id -> Text("Item ID: $id") }
                activity.details?.let { details ->
                    Spacer(Modifier.height(8.dp))
                    Text("Details:", fontWeight = FontWeight.Medium)
                    Text(details, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

/**
 * Error dialog for activity screen.
 */
@Composable
fun ActivityErrorDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
    )
}

/**
 * Get title for activity type.
 */
fun getActivityTitle(type: String): String = when (type) {
    "upload" -> "File Uploaded"
    "download" -> "File Downloaded"
    "create" -> "Item Created"
    "delete" -> "Item Deleted"
    "move" -> "Item Moved"
    "rename" -> "Item Renamed"
    "copy" -> "Item Copied"
    "restore" -> "Item Restored"
    "trash" -> "Moved to Trash"
    "star" -> "Item Starred"
    "share" -> "Item Shared"
    "unshare" -> "Share Removed"
    "share_access" -> "Share Accessed"
    "login" -> "Logged In"
    "logout" -> "Logged Out"
    "register" -> "Account Created"
    "password_change" -> "Password Changed"
    else -> type.replaceFirstChar { it.uppercaseChar() }
}

/**
 * Get icon and color for activity type.
 */
@Composable
fun getActivityIconAndColor(type: String): Pair<ImageVector, Color> = when (type) {
    "upload" -> Icons.Default.CloudUpload to MaterialTheme.colorScheme.primary
    "download" -> Icons.Default.CloudDownload to MaterialTheme.colorScheme.secondary
    "create" -> Icons.Default.CreateNewFolder to MaterialTheme.colorScheme.primary
    "delete" -> Icons.Default.Delete to MaterialTheme.colorScheme.error
    "move" -> Icons.AutoMirrored.Filled.DriveFileMove to MaterialTheme.colorScheme.tertiary
    "rename" -> Icons.Default.DriveFileRenameOutline to MaterialTheme.colorScheme.secondary
    "copy" -> Icons.Default.ContentCopy to MaterialTheme.colorScheme.tertiary
    "restore" -> Icons.Default.RestoreFromTrash to MaterialTheme.colorScheme.primary
    "trash" -> Icons.Default.Delete to MaterialTheme.colorScheme.error
    "star" -> Icons.Default.Star to Color(0xFFFFC107)
    "share" -> Icons.Default.Share to MaterialTheme.colorScheme.primary
    "unshare" -> Icons.Default.Share to MaterialTheme.colorScheme.error
    "login" -> Icons.AutoMirrored.Filled.Login to MaterialTheme.colorScheme.primary
    "logout" -> Icons.AutoMirrored.Filled.Logout to MaterialTheme.colorScheme.secondary
    else -> Icons.Default.History to MaterialTheme.colorScheme.onSurfaceVariant
}

// region Previews

@Preview
@Composable
internal fun ActivityCardPreview() {
    VaultStadioPreview {
        ActivityCard(
            activity = SampleActivity,
            onClick = {},
        )
    }
}

@Preview
@Composable
internal fun ActivityCardDeletePreview() {
    VaultStadioPreview {
        ActivityCard(
            activity = SampleActivityDelete,
            onClick = {},
        )
    }
}

@Preview
@Composable
internal fun ActivityCardLoginPreview() {
    VaultStadioPreview {
        ActivityCard(
            activity = SampleActivityLogin,
            onClick = {},
        )
    }
}

@Preview
@Composable
internal fun EmptyActivityStatePreview() {
    VaultStadioPreview {
        EmptyActivityState()
    }
}

@Preview
@Composable
internal fun ActivityDetailDialogPreview() {
    VaultStadioPreview {
        ActivityDetailDialog(
            activity = SampleActivity,
            onDismiss = {},
        )
    }
}

// endregion
