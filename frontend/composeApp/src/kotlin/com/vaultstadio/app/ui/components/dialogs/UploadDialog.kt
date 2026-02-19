/**
 * VaultStadio Upload Dialog
 *
 * Dialog for uploading files with progress tracking.
 */

package com.vaultstadio.app.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.i18n.LocalStrings

/**
 * Upload item state.
 */
data class UploadItem(
    val id: String,
    val fileName: String,
    val filePath: String,
    val size: Long,
    val progress: Float = 0f,
    val status: UploadStatus = UploadStatus.PENDING,
)

/**
 * Upload status.
 */
enum class UploadStatus {
    PENDING,
    UPLOADING,
    COMPLETED,
    FAILED,
}

/**
 * Upload dialog showing upload progress.
 */
@Composable
fun UploadDialog(
    isOpen: Boolean,
    uploadItems: List<UploadItem>,
    onDismiss: () -> Unit,
    onSelectFiles: () -> Unit,
    onRemoveItem: (String) -> Unit,
    onStartUpload: () -> Unit,
    onCancelUpload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isOpen) return

    val strings = LocalStrings.current
    val isUploading = uploadItems.any { it.status == UploadStatus.UPLOADING }
    val completedCount = uploadItems.count { it.status == UploadStatus.COMPLETED }
    val failedCount = uploadItems.count { it.status == UploadStatus.FAILED }

    AlertDialog(
        onDismissRequest = {
            if (!isUploading) onDismiss()
        },
        modifier = modifier.widthIn(max = 500.dp),
        icon = { Icon(Icons.Filled.CloudUpload, contentDescription = null) },
        title = { Text(strings.actionUploadFiles) },
        text = {
            Column {
                if (uploadItems.isEmpty()) {
                    // No files selected
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = strings.filesSelectToUpload,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(onClick = onSelectFiles) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.actionSelectFiles)
                            }
                        }
                    }
                } else {
                    // File list
                    Column {
                        // Progress summary
                        if (isUploading || completedCount > 0 || failedCount > 0) {
                            LinearProgressIndicator(
                                progress = {
                                    if (uploadItems.isEmpty()) {
                                        0f
                                    } else {
                                        uploadItems.sumOf { it.progress.toDouble() }.toFloat() / uploadItems.size
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = buildString {
                                    append("$completedCount / ${uploadItems.size}")
                                    if (failedCount > 0) {
                                        append(" ($failedCount failed)")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // File list
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(uploadItems) { item ->
                                UploadItemRow(
                                    item = item,
                                    onRemove = { onRemoveItem(item.id) },
                                    canRemove = item.status != UploadStatus.UPLOADING,
                                )
                            }
                        }

                        // Add more files button
                        if (!isUploading) {
                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(onClick = onSelectFiles) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.actionAddMoreFiles)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (uploadItems.isNotEmpty()) {
                if (isUploading) {
                    TextButton(
                        onClick = onCancelUpload,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text(strings.actionCancel)
                    }
                } else {
                    val allCompleted = uploadItems.all {
                        it.status == UploadStatus.COMPLETED || it.status == UploadStatus.FAILED
                    }

                    if (allCompleted) {
                        TextButton(onClick = onDismiss) {
                            Text(strings.actionDone)
                        }
                    } else {
                        Button(onClick = onStartUpload) {
                            Text(strings.actionUpload)
                        }
                    }
                }
            }
        },
        dismissButton = {
            if (!isUploading &&
                uploadItems.isNotEmpty() &&
                !uploadItems.all { it.status == UploadStatus.COMPLETED || it.status == UploadStatus.FAILED }
            ) {
                TextButton(onClick = onDismiss) {
                    Text(strings.actionCancel)
                }
            } else if (uploadItems.isEmpty()) {
                TextButton(onClick = onDismiss) {
                    Text(strings.actionClose)
                }
            }
        },
    )
}

/**
 * Individual upload item row.
 */
@Composable
private fun UploadItemRow(
    item: UploadItem,
    onRemove: () -> Unit,
    canRemove: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Status icon
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (item.status) {
                    UploadStatus.PENDING -> {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    UploadStatus.UPLOADING -> {
                        CircularProgressIndicator(
                            progress = { item.progress },
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    UploadStatus.COMPLETED -> {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Completed",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    UploadStatus.FAILED -> {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = "Failed",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // File info
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = item.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = formatFileSize(item.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }

            // Remove button
            if (canRemove) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/**
 * Format file size for display.
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> {
            val gb = bytes.toDouble() / (1024 * 1024 * 1024)
            val rounded = ((gb * 100).toLong() / 100.0)
            "$rounded GB"
        }
    }
}

/**
 * Create folder dialog.
 */
@Composable
fun CreateFolderDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onCreateFolder: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isOpen) return

    val strings = LocalStrings.current
    var folderName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = { Icon(Icons.Filled.CreateNewFolder, contentDescription = null) },
        title = { Text(strings.actionCreateFolder) },
        text = {
            Column {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = {
                        folderName = it
                        error = null
                    },
                    label = { Text(strings.folderName) },
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
                    if (folderName.isBlank()) {
                        error = strings.folderNameError
                    } else if (folderName.contains("/") || folderName.contains("\\")) {
                        error = strings.folderNameSlashError
                    } else {
                        onCreateFolder(folderName.trim())
                        onDismiss()
                    }
                },
            ) {
                Text(strings.actionCreateFolder)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.actionCancel)
            }
        },
    )
}
