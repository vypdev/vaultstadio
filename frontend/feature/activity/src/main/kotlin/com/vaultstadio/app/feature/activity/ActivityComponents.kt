package com.vaultstadio.app.feature.activity

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
import kotlin.time.Clock

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

private fun formatRelativeTime(instant: kotlinx.datetime.Instant): String {
    val now = Clock.System.now()
    val diff = now - instant
    val seconds = diff.inWholeSeconds
    val minutes = diff.inWholeMinutes
    val hours = diff.inWholeHours
    val days = diff.inWholeDays

    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> if (minutes == 1L) "1 minute ago" else "$minutes minutes ago"
        hours < 24 -> if (hours == 1L) "1 hour ago" else "$hours hours ago"
        days < 7 -> if (days == 1L) "Yesterday" else "$days days ago"
        days < 30 -> "${days / 7} weeks ago"
        days < 365 -> "${days / 30} months ago"
        else -> "${days / 365} years ago"
    }
}
