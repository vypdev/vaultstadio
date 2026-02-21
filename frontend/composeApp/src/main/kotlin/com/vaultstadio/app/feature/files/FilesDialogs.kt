/**
 * VaultStadio Files Screen - Dialogs
 *
 * Extracted dialog components for file operations.
 */

package com.vaultstadio.app.feature.files

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.core.resources.StringResources
import com.vaultstadio.app.feature.files.FilesMode

/**
 * Dialog for creating a new folder.
 */
@Composable
fun NewFolderDialog(
    folderName: String,
    onFolderNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    strings: StringResources,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.actionCreateFolder) },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = onFolderNameChange,
                label = { Text(strings.folderName) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = folderName.isNotBlank(),
            ) {
                Text(strings.actionDone)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.actionCancel)
            }
        },
    )
}

/**
 * Dialog for renaming an item.
 */
@Composable
fun RenameDialog(
    renameValue: String,
    onRenameValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    strings: StringResources,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.actionRename) },
        text = {
            OutlinedTextField(
                value = renameValue,
                onValueChange = onRenameValueChange,
                label = { Text("New name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = renameValue.isNotBlank(),
            ) {
                Text(strings.actionRename)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.actionCancel)
            }
        },
    )
}

/**
 * Dialog for moving an item.
 */
@Composable
fun MoveDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    strings: StringResources,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.actionMove) },
        text = { Text("Move to root folder?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(strings.actionMove)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.actionCancel)
            }
        },
    )
}

/**
 * Dialog for copying an item.
 */
@Composable
fun CopyDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    strings: StringResources,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.actionCopy) },
        text = { Text("Copy to root folder?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(strings.actionCopy)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.actionCancel)
            }
        },
    )
}

/**
 * Dialog for sharing an item.
 */
@Composable
fun ShareDialog(
    itemName: String?,
    shareUrl: String?,
    shareError: String?,
    isCreatingShare: Boolean,
    onCreateShare: () -> Unit,
    onDismiss: () -> Unit,
    strings: StringResources,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.shareTitle) },
        text = {
            Column {
                if (shareUrl != null) {
                    // Show created share link
                    Text("Share link created successfully!")
                    Text(
                        text = shareUrl,
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    // Show share creation UI
                    Text("Create a share link for this file:")
                    itemName?.let { name ->
                        Text(
                            text = name,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    if (shareError != null) {
                        Text(
                            text = shareError,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    if (isCreatingShare) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(top = 8.dp).size(24.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (shareUrl != null) {
                TextButton(onClick = onDismiss) {
                    Text(strings.actionDone)
                }
            } else {
                TextButton(
                    onClick = onCreateShare,
                    enabled = !isCreatingShare,
                ) {
                    Text(strings.shareCreateLink)
                }
            }
        },
        dismissButton = {
            if (shareUrl == null) {
                TextButton(onClick = onDismiss) {
                    Text(strings.actionCancel)
                }
            }
        },
    )
}

/**
 * Confirmation dialog for restoring an item from trash.
 */
@Composable
fun RestoreConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    strings: StringResources,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.trashRestoreItem) },
        text = { Text(strings.trashRestoreConfirmMessage) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(strings.trashRestoreItem)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.actionCancel)
            }
        },
    )
}

/**
 * Confirmation dialog for permanently deleting an item from trash.
 */
@Composable
fun DeletePermanentlyConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    strings: StringResources,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.trashDeletePermanently) },
        text = { Text(strings.trashDeletePermanentlyConfirmMessage) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(strings.trashDeletePermanently)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.actionCancel)
            }
        },
    )
}

/**
 * State holder for files screen dialogs. Used by [FilesScreenDialogs] to avoid passing many params.
 */
data class FilesScreenDialogState(
    val showNewFolderDialog: Boolean = false,
    val newFolderName: String = "",
    val showRenameDialog: Boolean = false,
    val renameValue: String = "",
    val showMoveDialog: Boolean = false,
    val showCopyDialog: Boolean = false,
    val showShareDialog: Boolean = false,
    val itemToRestore: String? = null,
    val itemToDeletePermanently: String? = null,
)

/**
 * Renders all files screen dialogs (new folder, rename, move, copy, share, restore, delete permanently).
 * Call from [FilesContent] with state and viewModel so the main content file stays under the line limit.
 */
@Composable
fun FilesScreenDialogs(
    state: FilesScreenDialogState,
    onStateChange: (FilesScreenDialogState) -> Unit,
    viewModel: FilesViewModel,
    mode: FilesMode,
    strings: StringResources,
) {
    if (state.showNewFolderDialog) {
        NewFolderDialog(
            folderName = state.newFolderName,
            onFolderNameChange = { onStateChange(state.copy(newFolderName = it)) },
            onConfirm = {
                if (state.newFolderName.isNotBlank()) {
                    viewModel.createFolder(state.newFolderName) {
                        onStateChange(state.copy(showNewFolderDialog = false, newFolderName = ""))
                    }
                }
            },
            onDismiss = { onStateChange(state.copy(showNewFolderDialog = false, newFolderName = "")) },
            strings = strings,
        )
    }
    if (state.showRenameDialog && viewModel.selectedInfoItem != null) {
        RenameDialog(
            renameValue = state.renameValue,
            onRenameValueChange = { onStateChange(state.copy(renameValue = it)) },
            onConfirm = {
                if (state.renameValue.isNotBlank()) {
                    viewModel.renameItem(viewModel.selectedInfoItem!!.id, state.renameValue)
                    onStateChange(state.copy(showRenameDialog = false, renameValue = ""))
                    viewModel.hideItemInfo()
                }
            },
            onDismiss = { onStateChange(state.copy(showRenameDialog = false, renameValue = "")) },
            strings = strings,
        )
    }
    if (state.showMoveDialog && viewModel.selectedInfoItem != null) {
        MoveDialog(
            onConfirm = {
                viewModel.selectedInfoItem?.let { item ->
                    viewModel.toggleItemSelection(item.id)
                    viewModel.batchMove(null)
                    viewModel.clearSelection()
                }
                onStateChange(state.copy(showMoveDialog = false))
                viewModel.hideItemInfo()
            },
            onDismiss = { onStateChange(state.copy(showMoveDialog = false)) },
            strings = strings,
        )
    }
    if (state.showCopyDialog && viewModel.selectedInfoItem != null) {
        CopyDialog(
            onConfirm = {
                viewModel.selectedInfoItem?.let { item ->
                    viewModel.toggleItemSelection(item.id)
                    viewModel.batchCopy(null)
                    viewModel.clearSelection()
                }
                onStateChange(state.copy(showCopyDialog = false))
                viewModel.hideItemInfo()
            },
            onDismiss = { onStateChange(state.copy(showCopyDialog = false)) },
            strings = strings,
        )
    }
    if (state.itemToRestore != null) {
        RestoreConfirmDialog(
            onConfirm = {
                viewModel.restoreItem(state.itemToRestore!!)
                onStateChange(state.copy(itemToRestore = null))
                viewModel.hideItemInfo()
            },
            onDismiss = { onStateChange(state.copy(itemToRestore = null)) },
            strings = strings,
        )
    }
    if (state.itemToDeletePermanently != null) {
        DeletePermanentlyConfirmDialog(
            onConfirm = {
                viewModel.deleteItemPermanently(state.itemToDeletePermanently!!)
                onStateChange(state.copy(itemToDeletePermanently = null))
                viewModel.hideItemInfo()
            },
            onDismiss = { onStateChange(state.copy(itemToDeletePermanently = null)) },
            strings = strings,
        )
    }
    if (state.showShareDialog && viewModel.selectedInfoItem != null) {
        ShareDialog(
            itemName = viewModel.selectedInfoItem?.name,
            shareUrl = viewModel.lastCreatedShare?.let { viewModel.getShareUrl(it) },
            shareError = viewModel.shareError,
            isCreatingShare = viewModel.isCreatingShare,
            onCreateShare = {
                viewModel.selectedInfoItem?.let { item ->
                    viewModel.createShare(itemId = item.id)
                }
            },
            onDismiss = {
                onStateChange(state.copy(showShareDialog = false))
                viewModel.clearShareError()
                if (viewModel.lastCreatedShare != null) viewModel.clearLastCreatedShare()
            },
            strings = strings,
        )
    }
}
