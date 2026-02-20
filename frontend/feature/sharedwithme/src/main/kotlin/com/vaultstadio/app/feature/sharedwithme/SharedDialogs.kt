package com.vaultstadio.app.feature.sharedwithme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.storage.model.StorageItem

@Composable
fun ItemDetailsDialog(
    item: StorageItem,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Type: ${if (item.isFolder) "Folder" else "File"}")
                Text("Size: ${formatFileSize(item.size)}")
                Text("Modified: ${formatRelativeTime(item.updatedAt)}")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
fun DownloadReadyDialog(
    url: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Download Ready") },
        text = {
            Column {
                Text("Your download is ready.")
                Spacer(Modifier.height(8.dp))
                Text(
                    url.take(50) + if (url.length > 50) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
    )
}

@Composable
fun SharedErrorDialog(
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

private fun formatFileSize(sizeInBytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        sizeInBytes >= gb -> "${(sizeInBytes / gb * 10).toInt() / 10.0} GB"
        sizeInBytes >= mb -> "${(sizeInBytes / mb * 10).toInt() / 10.0} MB"
        sizeInBytes >= kb -> "${(sizeInBytes / kb * 10).toInt() / 10.0} KB"
        else -> "$sizeInBytes B"
    }
}

private fun formatRelativeTime(instant: kotlinx.datetime.Instant): String {
    val now = kotlin.time.Clock.System.now()
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
