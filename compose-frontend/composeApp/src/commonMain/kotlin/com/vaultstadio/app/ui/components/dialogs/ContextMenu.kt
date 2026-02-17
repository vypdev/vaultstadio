/**
 * VaultStadio Context Menu Components
 *
 * Context menu dialog for file operations.
 */

package com.vaultstadio.app.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.model.StorageItem

/**
 * Context menu dialog for file/folder operations.
 */
@Composable
fun ItemContextMenuDialog(
    item: StorageItem,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onStar: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit = {},
    onTrash: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.widthIn(min = 280.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (item.isFolder) Icons.Filled.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        },
        text = {
            Column {
                // Rename
                ContextMenuItem(
                    icon = Icons.Filled.Edit,
                    text = "Rename",
                    onClick = {
                        onRename()
                        onDismiss()
                    },
                )

                // Move
                ContextMenuItem(
                    icon = Icons.AutoMirrored.Filled.DriveFileMove,
                    text = "Move to...",
                    onClick = {
                        onMove()
                        onDismiss()
                    },
                )

                // Star/Unstar
                ContextMenuItem(
                    icon = if (item.isStarred) Icons.Filled.StarBorder else Icons.Filled.Star,
                    text = if (item.isStarred) "Remove from starred" else "Add to starred",
                    onClick = onStar,
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Share (only for files)
                if (!item.isFolder) {
                    ContextMenuItem(
                        icon = Icons.Filled.Share,
                        text = "Share",
                        onClick = {
                            onShare()
                            onDismiss()
                        },
                    )
                }

                // Download (only for files)
                if (!item.isFolder) {
                    ContextMenuItem(
                        icon = Icons.Filled.Download,
                        text = "Download",
                        onClick = {
                            onDownload()
                            onDismiss()
                        },
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Trash
                ContextMenuItem(
                    icon = Icons.Filled.Delete,
                    text = "Move to trash",
                    onClick = onTrash,
                    isDestructive = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

/**
 * Individual context menu item.
 */
@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDestructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

/**
 * Rename dialog.
 */
@Composable
fun RenameDialog(
    currentName: String,
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isOpen) return

    var newName by remember(currentName) { mutableStateOf(currentName) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
        title = { Text("Rename") },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        error = null
                    },
                    label = { Text("Name") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        newName.isBlank() -> error = "Name cannot be empty"
                        newName.contains("/") || newName.contains("\\") -> error = "Name cannot contain / or \\"
                        newName == currentName -> onDismiss()
                        else -> {
                            onRename(newName.trim())
                            onDismiss()
                        }
                    }
                },
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

/**
 * Share dialog for creating share links.
 */
@Composable
fun ShareDialog(
    itemName: String,
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onCreateShare: (expirationDays: Int?, password: String?, maxDownloads: Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isOpen) return

    var password by remember { mutableStateOf("") }
    var usePassword by remember { mutableStateOf(false) }
    var expirationDays by remember { mutableStateOf("7") }
    var useExpiration by remember { mutableStateOf(true) }
    var maxDownloads by remember { mutableStateOf("") }
    var useMaxDownloads by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.widthIn(max = 400.dp),
        icon = { Icon(Icons.Filled.Share, contentDescription = null) },
        title = { Text("Share \"$itemName\"") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Expiration
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = useExpiration,
                        onCheckedChange = { useExpiration = it },
                    )
                    Text("Expires after")
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = expirationDays,
                        onValueChange = { expirationDays = it.filter { c -> c.isDigit() } },
                        enabled = useExpiration,
                        singleLine = true,
                        modifier = Modifier.width(60.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("days")
                }

                // Password
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = usePassword,
                        onCheckedChange = { usePassword = it },
                    )
                    Text("Password protect")
                }

                if (usePassword) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Max downloads
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = useMaxDownloads,
                        onCheckedChange = { useMaxDownloads = it },
                    )
                    Text("Limit downloads to")
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = maxDownloads,
                        onValueChange = { maxDownloads = it.filter { c -> c.isDigit() } },
                        enabled = useMaxDownloads,
                        singleLine = true,
                        modifier = Modifier.width(60.dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreateShare(
                        if (useExpiration) expirationDays.toIntOrNull() else null,
                        if (usePassword && password.isNotBlank()) password else null,
                        if (useMaxDownloads) maxDownloads.toIntOrNull() else null,
                    )
                    onDismiss()
                },
            ) {
                Text("Create Link")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
