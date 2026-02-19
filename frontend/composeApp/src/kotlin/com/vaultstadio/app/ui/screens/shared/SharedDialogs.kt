/**
 * VaultStadio Shared Dialogs
 *
 * Dialog components for sharing-related screens.
 */

package com.vaultstadio.app.ui.screens.shared

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
import com.vaultstadio.app.domain.model.ItemType
import com.vaultstadio.app.domain.model.StorageItem
import com.vaultstadio.app.domain.model.Visibility
import com.vaultstadio.app.ui.theme.VaultStadioPreview
import com.vaultstadio.app.utils.formatFileSize
import com.vaultstadio.app.utils.formatRelativeTime
import kotlinx.datetime.Clock
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

private val SampleStorageItem = StorageItem(
    id = "item-1",
    name = "example-document.pdf",
    path = "/documents/example-document.pdf",
    type = ItemType.FILE,
    parentId = "folder-1",
    size = 1024 * 1024 * 10, // 10 MB
    mimeType = "application/pdf",
    visibility = Visibility.SHARED,
    isStarred = false,
    isTrashed = false,
    createdAt = Clock.System.now().minus(7.days),
    updatedAt = Clock.System.now().minus(1.hours),
    metadata = null,
)

/**
 * Dialog showing item details.
 */
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

/**
 * Dialog showing download ready notification.
 */
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

/**
 * Dialog showing link copied confirmation.
 */
@Composable
fun LinkCopiedDialog(
    link: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link Copied") },
        text = {
            Column {
                Text("The share link has been copied to your clipboard:")
                Spacer(Modifier.height(8.dp))
                Text(
                    link,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
    )
}

/**
 * Generic error dialog.
 */
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

// region Previews

@Preview
@Composable
internal fun ItemDetailsDialogPreview() {
    VaultStadioPreview {
        ItemDetailsDialog(
            item = SampleStorageItem,
            onDismiss = {},
        )
    }
}

@Preview
@Composable
internal fun DownloadReadyDialogPreview() {
    VaultStadioPreview {
        DownloadReadyDialog(
            url = "https://vaultstadio.example.com/download/abc123xyz",
            onDismiss = {},
        )
    }
}

@Preview
@Composable
internal fun LinkCopiedDialogPreview() {
    VaultStadioPreview {
        LinkCopiedDialog(
            link = "https://vaultstadio.example.com/share/abc123xyz",
            onDismiss = {},
        )
    }
}

@Preview
@Composable
internal fun SharedErrorDialogPreview() {
    VaultStadioPreview {
        SharedErrorDialog(
            message = "Failed to load shared items. Please try again later.",
            onDismiss = {},
        )
    }
}

// endregion
