package com.vaultstadio.app.feature.sharedwithme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.core.resources.Strings
import com.vaultstadio.app.core.resources.strings
import kotlinx.datetime.Instant
import kotlin.time.Clock

@Composable
fun SharedWithMeItemCard(
    sharedItem: SharedWithMeItem,
    onItemClick: () -> Unit,
    onDownload: () -> Unit,
    onRemoveShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onItemClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ItemIcon(isFolder = sharedItem.item.isFolder)
            Spacer(Modifier.width(12.dp))
            ItemDetails(sharedItem = sharedItem, modifier = Modifier.weight(1f))
            ItemMenu(
                isFolder = sharedItem.item.isFolder,
                showMenu = showMenu,
                onShowMenu = { showMenu = true },
                onHideMenu = { showMenu = false },
                onDownload = onDownload,
                onRemoveShare = onRemoveShare,
            )
        }
    }
}

@Composable
private fun ItemIcon(isFolder: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isFolder) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        modifier = Modifier.size(48.dp),
    ) {
        Icon(
            imageVector = if (isFolder) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.padding(12.dp),
            tint = if (isFolder) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
        )
    }
}

@Composable
private fun ItemDetails(
    sharedItem: SharedWithMeItem,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            sharedItem.item.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Person,
                null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                sharedItem.sharedBy,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatRelativeTime(sharedItem.sharedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!sharedItem.item.isFolder) {
                Text(
                    " • ${formatFileSize(sharedItem.item.size)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (sharedItem.permissions.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            PermissionsBadges(permissions = sharedItem.permissions)
        }
    }
}

@Composable
private fun PermissionsBadges(permissions: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        permissions.take(3).forEach { permission ->
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    permission,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ItemMenu(
    isFolder: Boolean,
    showMenu: Boolean,
    onShowMenu: () -> Unit,
    onHideMenu: () -> Unit,
    onDownload: () -> Unit,
    onRemoveShare: () -> Unit,
) {
    Box {
        IconButton(onClick = onShowMenu) {
            Icon(Icons.Default.MoreVert, "More options")
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = onHideMenu) {
            if (!isFolder) {
                DropdownMenuItem(
                    text = { Text(Strings.resources.actionDownload) },
                    onClick = {
                        onHideMenu()
                        onDownload()
                    },
                    leadingIcon = { Icon(Icons.Default.Download, null) },
                )
            }
            DropdownMenuItem(
                text = { Text(Strings.resources.commonRemove) },
                onClick = {
                    onHideMenu()
                    onRemoveShare()
                },
                leadingIcon = {
                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.error)
                },
            )
        }
    }
}

@Composable
fun EmptySharedWithMeState(modifier: Modifier = Modifier) {
    val strings = strings()

    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.People,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            strings.shareNoShares,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Files and folders shared with you will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
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

private fun formatRelativeTime(instant: Instant): String {
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
