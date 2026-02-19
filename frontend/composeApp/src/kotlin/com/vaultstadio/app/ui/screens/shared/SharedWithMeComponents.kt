/**
 * VaultStadio SharedWithMe Components
 *
 * UI components specific to the SharedWithMe screen.
 */

package com.vaultstadio.app.ui.screens.shared

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
import com.vaultstadio.app.domain.model.ItemType
import com.vaultstadio.app.domain.model.StorageItem
import com.vaultstadio.app.domain.model.Visibility
import com.vaultstadio.app.i18n.Strings
import com.vaultstadio.app.i18n.strings
import com.vaultstadio.app.ui.screens.SharedWithMeItem
import com.vaultstadio.app.ui.theme.VaultStadioPreview
import com.vaultstadio.app.utils.formatFileSize
import com.vaultstadio.app.utils.formatRelativeTime
import kotlin.time.Clock
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

private val SampleFileItem = StorageItem(
    id = "file-1",
    name = "document.pdf",
    path = "/documents/document.pdf",
    type = ItemType.FILE,
    parentId = "folder-1",
    size = 1024 * 1024 * 5, // 5 MB
    mimeType = "application/pdf",
    visibility = Visibility.SHARED,
    isStarred = false,
    isTrashed = false,
    createdAt = Clock.System.now().minus(5.days),
    updatedAt = Clock.System.now().minus(2.hours),
    metadata = null,
)

private val SampleFolderItem = StorageItem(
    id = "folder-1",
    name = "Shared Folder",
    path = "/documents/shared-folder",
    type = ItemType.FOLDER,
    parentId = null,
    size = 0,
    mimeType = null,
    visibility = Visibility.SHARED,
    isStarred = false,
    isTrashed = false,
    createdAt = Clock.System.now().minus(10.days),
    updatedAt = Clock.System.now().minus(1.days),
    metadata = null,
)

private val SampleSharedWithMeItem = SharedWithMeItem(
    item = SampleFileItem,
    sharedBy = "John Doe",
    sharedByEmail = "john.doe@example.com",
    sharedAt = Clock.System.now().minus(3.days),
    permissions = listOf("read", "download"),
)

private val SampleSharedWithMeFolderItem = SharedWithMeItem(
    item = SampleFolderItem,
    sharedBy = "Jane Smith",
    sharedByEmail = "jane.smith@example.com",
    sharedAt = Clock.System.now().minus(12.hours),
    permissions = listOf("read", "write"),
)

/**
 * Card displaying a shared item.
 */
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

/**
 * Empty state for when no items are shared with the user.
 */
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

// region Previews

@Preview
@Composable
internal fun SharedWithMeItemCardPreview() {
    VaultStadioPreview {
        Column {
            SharedWithMeItemCard(
                sharedItem = SampleSharedWithMeItem,
                onItemClick = {},
                onDownload = {},
                onRemoveShare = {},
            )
            SharedWithMeItemCard(
                sharedItem = SampleSharedWithMeFolderItem,
                onItemClick = {},
                onDownload = {},
                onRemoveShare = {},
            )
        }
    }
}

@Preview
@Composable
internal fun EmptySharedWithMeStatePreview() {
    VaultStadioPreview {
        EmptySharedWithMeState()
    }
}

// endregion
