/**
 * Context menu for a file or folder (long-press / Google Drive–style).
 */

package com.vaultstadio.app.feature.files

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.vaultstadio.app.domain.storage.model.ItemType
import com.vaultstadio.app.domain.storage.model.StorageItem
import com.vaultstadio.app.feature.main.MainComponent

@Composable
internal fun FileContextMenuItems(
    item: StorageItem,
    mode: MainComponent.FilesMode,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onDownload: () -> Unit,
    onStar: () -> Unit,
    onDelete: () -> Unit,
    onRestore: (() -> Unit)?,
    onDeletePermanently: (() -> Unit)?,
) {
    DropdownMenuItem(
        text = { Text(if (item.type == ItemType.FOLDER) "Open" else "Open / Preview") },
        onClick = onOpen,
        leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
    )
    DropdownMenuItem(
        text = { Text("Rename") },
        onClick = onRename,
        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
    )
    if (mode == MainComponent.FilesMode.ALL) {
        DropdownMenuItem(
            text = { Text("Move to") },
            onClick = onMove,
            leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) },
        )
        DropdownMenuItem(
            text = { Text("Copy to") },
            onClick = onCopy,
            leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) },
        )
    }
    if (mode != MainComponent.FilesMode.TRASH && item.type != ItemType.FOLDER) {
        DropdownMenuItem(
            text = { Text("Download") },
            onClick = onDownload,
            leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
        )
    }
    if (mode != MainComponent.FilesMode.TRASH) {
        DropdownMenuItem(
            text = { Text("Add to starred") },
            onClick = onStar,
            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
        )
    }
    if (mode == MainComponent.FilesMode.TRASH) {
        onRestore?.let { restore ->
            DropdownMenuItem(
                text = { Text("Restore") },
                onClick = restore,
                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
            )
        }
        onDeletePermanently?.let { delete ->
            DropdownMenuItem(
                text = { Text("Delete permanently") },
                onClick = delete,
                leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
            )
        }
    } else {
        DropdownMenuItem(
            text = { Text("Move to trash") },
            onClick = onDelete,
            leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
        )
    }
}
