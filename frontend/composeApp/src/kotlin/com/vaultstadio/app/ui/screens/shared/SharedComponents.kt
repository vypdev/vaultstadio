/**
 * VaultStadio Shared Components
 *
 * Reusable UI components for sharing-related screens.
 */

package com.vaultstadio.app.ui.screens.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.share.model.ShareLink
import com.vaultstadio.app.ui.theme.VaultStadioPreview
import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Duration.Companion.days

private val SampleShareLink = ShareLink(
    id = "share-1",
    itemId = "item-1",
    token = "abc123xyz",
    url = "https://vaultstadio.example.com/share/abc123xyz",
    expiresAt = Clock.System.now().plus(7.days),
    hasPassword = true,
    maxDownloads = 10,
    downloadCount = 3,
    isActive = true,
    createdAt = Clock.System.now().minus(2.days),
    createdBy = "user-1",
    sharedWithUsers = emptyList(),
)

private val SampleExpiredShareLink = ShareLink(
    id = "share-2",
    itemId = "item-2",
    token = "expired123",
    url = "https://vaultstadio.example.com/share/expired123",
    expiresAt = Clock.System.now().minus(1.days),
    hasPassword = false,
    maxDownloads = null,
    downloadCount = 5,
    isActive = false,
    createdAt = Clock.System.now().minus(30.days),
    createdBy = "user-2",
    sharedWithUsers = emptyList(),
)

/**
 * Card displaying a shared link with actions.
 */
@Composable
fun ShareLinkCard(
    share: ShareLink,
    onCopyLink: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (share.isActive) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ShareLinkHeader(
                share = share,
                onCopyLink = onCopyLink,
                onDeleteClick = { showDeleteConfirm = true },
            )

            share.expiresAt?.let { expiresAt ->
                Spacer(modifier = Modifier.height(8.dp))
                ExpirationInfo(expiresAt = expiresAt)
            }

            Text(
                text = "Created: ${formatDateTime(share.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }

    if (showDeleteConfirm) {
        DeleteShareConfirmDialog(
            onConfirm = {
                onDelete()
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

@Composable
private fun ShareLinkHeader(
    share: ShareLink,
    onCopyLink: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = Icons.Filled.Link,
                contentDescription = null,
                tint = if (share.isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                },
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = share.url,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(isActive = share.isActive, hasPassword = share.hasPassword)
                    Spacer(modifier = Modifier.width(8.dp))
                    DownloadCountText(share = share)
                }
            }
        }

        Row {
            IconButton(onClick = onCopyLink) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy link")
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete share",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun DownloadCountText(share: ShareLink) {
    Text(
        text = "${share.downloadCount} downloads${share.maxDownloads?.let { " / $it" } ?: ""}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    )
}

@Composable
private fun ExpirationInfo(expiresAt: Instant) {
    val isExpired = expiresAt < Clock.System.now()

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isExpired) Icons.Filled.Warning else Icons.Filled.Schedule,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isExpired) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            },
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (isExpired) "Expired" else "Expires: ${formatDateTime(expiresAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = if (isExpired) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            },
        )
    }
}

/**
 * Status badge for share link.
 */
@Composable
fun StatusBadge(
    isActive: Boolean,
    hasPassword: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
        ) {
            Text(
                text = if (isActive) "Active" else "Inactive",
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        if (hasPassword) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Password protected",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

/**
 * Confirmation dialog for deleting a share.
 */
@Composable
fun DeleteShareConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
        title = { Text("Delete Share Link?") },
        text = {
            Text(
                "This will permanently delete the share link. Anyone with the link " +
                    "will no longer be able to access the file.",
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * Format datetime for display.
 */
fun formatDateTime(instant: Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.monthNumber}/${localDateTime.dayOfMonth}/${localDateTime.year} " +
        "${localDateTime.hour.toString().padStart(2, '0')}:" +
        localDateTime.minute.toString().padStart(2, '0')
}

// region Previews

@Preview
@Composable
internal fun ShareLinkCardPreview() {
    VaultStadioPreview {
        Column {
            ShareLinkCard(
                share = SampleShareLink,
                onCopyLink = {},
                onDelete = {},
            )
            ShareLinkCard(
                share = SampleExpiredShareLink,
                onCopyLink = {},
                onDelete = {},
            )
        }
    }
}

@Preview
@Composable
internal fun StatusBadgePreview() {
    VaultStadioPreview {
        Column {
            StatusBadge(isActive = true, hasPassword = false)
            StatusBadge(isActive = true, hasPassword = true)
            StatusBadge(isActive = false, hasPassword = false)
            StatusBadge(isActive = false, hasPassword = true)
        }
    }
}

@Preview
@Composable
internal fun DeleteShareConfirmDialogPreview() {
    VaultStadioPreview {
        DeleteShareConfirmDialog(
            onConfirm = {},
            onDismiss = {},
        )
    }
}

// endregion
