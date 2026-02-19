/**
 * VaultStadio Version History Dialogs
 *
 * Dialog components for the Version History screen.
 */

package com.vaultstadio.app.ui.screens.version

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.version.model.FileVersion
import com.vaultstadio.app.domain.version.model.VersionDiff
import com.vaultstadio.app.ui.theme.VaultStadioPreview
import com.vaultstadio.app.utils.formatFileSize
import com.vaultstadio.app.utils.formatRelativeTime
import kotlin.time.Clock
import org.jetbrains.compose.ui.tooling.preview.Preview

private val SampleVersion = FileVersion(
    id = "version-1",
    itemId = "item-1",
    versionNumber = 1,
    size = 1024L * 1024L,
    createdAt = Clock.System.now(),
    createdBy = "john.doe",
    comment = "Initial version",
    checksum = "abc123",
    isLatest = true,
)

private val SampleDiff = VersionDiff(
    fromVersion = 1,
    toVersion = 2,
    sizeChange = 1024L,
    additions = 45,
    deletions = 12,
    isBinary = false,
)

/**
 * Dialog for confirming version restoration.
 */
@Composable
fun RestoreVersionDialog(
    version: FileVersion,
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.History, null) },
        title = { Text("Restore Version ${version.versionNumber}?") },
        text = {
            Column {
                Text("This will create a new version with the content from v${version.versionNumber}.")
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comment (optional)") },
                    placeholder = { Text("Restored from v${version.versionNumber}") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(comment.ifBlank { null }) }) {
                Text("Restore")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * Dialog for confirming version deletion.
 */
@Composable
fun DeleteVersionDialog(
    version: FileVersion,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Version ${version.versionNumber}?") },
        text = {
            Text("This action cannot be undone. The version data will be permanently removed.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
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
 * Dialog showing version details.
 */
@Composable
fun VersionDetailsDialog(
    version: FileVersion,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Version ${version.versionNumber} Details") },
        text = {
            Column {
                DetailRow("Size", formatFileSize(version.size))
                DetailRow("Created", formatRelativeTime(version.createdAt))
                DetailRow("Version", version.versionNumber.toString())
                version.comment?.let { DetailRow("Comment", it) }
                if (version.isRestore) {
                    DetailRow("Restored from", "v${version.restoredFrom}")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(Modifier.height(4.dp))
}

/**
 * Dialog showing diff comparison.
 */
@Composable
fun DiffCompareDialog(
    diff: VersionDiff,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Version Comparison") },
        text = {
            Column {
                Text("Comparing version ${diff.fromVersion} to ${diff.toVersion}")
                Spacer(Modifier.height(8.dp))
                Text("Additions: ${diff.additions}")
                Text("Deletions: ${diff.deletions}")
                Text("Size change: ${diff.sizeChange} bytes")
                if (diff.isBinary) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Binary file - detailed diff not available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

/**
 * Dialog for cleanup configuration.
 */
@Composable
fun CleanupDialog(
    onConfirm: (Int?, Int?, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var keepCount by remember { mutableStateOf("10") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cleanup Old Versions") },
        text = {
            Column {
                Text("Keep the most recent versions and delete older ones.")
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = keepCount,
                    onValueChange = { keepCount = it.filter { c -> c.isDigit() } },
                    label = { Text("Keep versions") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(null, null, keepCount.toIntOrNull() ?: 10) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Cleanup")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * Generic error dialog.
 */
@Composable
fun VersionErrorDialog(
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
internal fun RestoreVersionDialogPreview() {
    VaultStadioPreview {
        RestoreVersionDialog(
            version = SampleVersion,
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
internal fun DeleteVersionDialogPreview() {
    VaultStadioPreview {
        DeleteVersionDialog(
            version = SampleVersion,
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
internal fun VersionDetailsDialogPreview() {
    VaultStadioPreview {
        VersionDetailsDialog(
            version = SampleVersion,
            onDismiss = {},
        )
    }
}

@Preview
@Composable
internal fun DiffCompareDialogPreview() {
    VaultStadioPreview {
        DiffCompareDialog(
            diff = SampleDiff,
            onDismiss = {},
        )
    }
}

@Preview
@Composable
internal fun CleanupDialogPreview() {
    VaultStadioPreview {
        CleanupDialog(
            onConfirm = { _, _, _ -> },
            onDismiss = {},
        )
    }
}

@Preview
@Composable
internal fun VersionErrorDialogPreview() {
    VaultStadioPreview {
        VersionErrorDialog(
            message = "Failed to restore version. Please try again.",
            onDismiss = {},
        )
    }
}

// endregion
