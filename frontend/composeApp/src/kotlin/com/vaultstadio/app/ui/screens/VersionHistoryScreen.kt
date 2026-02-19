/**
 * VaultStadio Version History Screen
 *
 * Screen for viewing file version history, comparing versions, and restoring.
 */

package com.vaultstadio.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.version.model.FileVersion
import com.vaultstadio.app.domain.version.model.FileVersionHistory
import com.vaultstadio.app.domain.version.model.VersionDiff
import com.vaultstadio.app.i18n.strings
import com.vaultstadio.app.ui.screens.version.DiffView
import com.vaultstadio.app.ui.screens.version.EmptyVersionState
import com.vaultstadio.app.ui.screens.version.SummaryItem
import com.vaultstadio.app.ui.screens.version.VersionCard
import com.vaultstadio.app.utils.formatFileSize
import com.vaultstadio.app.utils.formatRelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionHistoryScreen(
    itemId: String,
    itemName: String,
    versionHistory: FileVersionHistory?,
    selectedVersion: FileVersion?,
    versionDiff: VersionDiff?,
    downloadUrl: String?,
    isLoading: Boolean,
    error: String?,
    onLoadHistory: (String) -> Unit,
    onGetVersion: (String, Int) -> Unit,
    onRestoreVersion: (String, Int, String?) -> Unit,
    onDeleteVersion: (String) -> Unit,
    onCompareVersions: (String, Int, Int) -> Unit,
    onDownloadVersion: (String, Int) -> Unit,
    onCleanupVersions: (String, Int?, Int?, Int) -> Unit,
    onClearSelectedVersion: () -> Unit,
    onClearDownloadUrl: () -> Unit,
    onClearDiff: () -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = strings()
    var showRestoreDialog by remember { mutableStateOf<FileVersion?>(null) }
    var showCleanupDialog by remember { mutableStateOf(false) }
    var showDiffDialog by remember { mutableStateOf(false) }
    var selectedVersions by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var restoreComment by remember { mutableStateOf("") }
    var versionToDelete by remember { mutableStateOf<FileVersion?>(null) }
    var versionToView by remember { mutableStateOf<FileVersion?>(null) }

    LaunchedEffect(itemId) {
        onLoadHistory(itemId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(strings.versionTitle)
                        Text(
                            itemName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCleanupDialog = true }) {
                        Icon(Icons.Default.CleaningServices, contentDescription = "Cleanup")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                versionHistory == null || versionHistory.versions.isEmpty() -> {
                    EmptyVersionState(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Summary card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                SummaryItem(
                                    label = strings.versionVersions,
                                    value = versionHistory.totalVersions.toString(),
                                )
                                SummaryItem(
                                    label = strings.versionTotalSize,
                                    value = formatFileSize(versionHistory.totalSize),
                                )
                            }
                        }

                        // Version list
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(versionHistory.versions) { version ->
                                VersionCard(
                                    version = version,
                                    onRestore = { showRestoreDialog = version },
                                    onDownload = { onDownloadVersion(itemId, version.versionNumber) },
                                    onViewDetails = {
                                        versionToView = version
                                        onGetVersion(itemId, version.versionNumber)
                                    },
                                    onDelete = { versionToDelete = version },
                                    onCompare = {
                                        val prevVersion = versionHistory.versions
                                            .find { it.versionNumber == version.versionNumber - 1 }
                                        if (prevVersion != null) {
                                            selectedVersions = prevVersion.versionNumber to version.versionNumber
                                            onCompareVersions(itemId, prevVersion.versionNumber, version.versionNumber)
                                            showDiffDialog = true
                                        }
                                    },
                                    hasPreviousVersion = versionHistory.versions.any {
                                        it.versionNumber == version.versionNumber - 1
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Restore dialog
    showRestoreDialog?.let { version ->
        AlertDialog(
            onDismissRequest = { showRestoreDialog = null },
            title = { Text("${strings.versionRestoreConfirm} ${version.versionNumber}") },
            text = {
                Column {
                    Text(strings.versionRestoreDesc.replace("%d", version.versionNumber.toString()))
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = restoreComment,
                        onValueChange = { restoreComment = it },
                        label = { Text(strings.versionCommentOptional) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onRestoreVersion(
                        itemId,
                        version.versionNumber,
                        restoreComment.takeIf { it.isNotBlank() },
                    )
                    showRestoreDialog = null
                    restoreComment = ""
                }) {
                    Text(strings.versionRestore)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = null }) {
                    Text(strings.actionCancel)
                }
            },
        )
    }

    // Cleanup dialog
    if (showCleanupDialog) {
        var maxVersions by remember { mutableStateOf("") }
        var maxAgeDays by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCleanupDialog = false },
            title = { Text(strings.versionCleanupOld) },
            text = {
                Column {
                    Text(strings.versionCleanupDesc)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = maxVersions,
                        onValueChange = { maxVersions = it.filter { c -> c.isDigit() } },
                        label = { Text(strings.versionKeepLast) },
                        placeholder = { Text(strings.versionKeepLastPlaceholder) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = maxAgeDays,
                        onValueChange = { maxAgeDays = it.filter { c -> c.isDigit() } },
                        label = { Text(strings.versionDeleteOlderThan) },
                        placeholder = { Text(strings.versionDeleteOlderThanPlaceholder) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onCleanupVersions(
                        itemId,
                        maxVersions.toIntOrNull(),
                        maxAgeDays.toIntOrNull(),
                        1,
                    )
                    showCleanupDialog = false
                }) {
                    Text(strings.versionCleanup)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanupDialog = false }) {
                    Text(strings.actionCancel)
                }
            },
        )
    }

    // Diff dialog
    if (showDiffDialog && versionDiff != null && selectedVersions != null) {
        AlertDialog(
            onDismissRequest = { showDiffDialog = false },
            title = {
                Text("Compare v${selectedVersions!!.first} → v${selectedVersions!!.second}")
            },
            text = {
                DiffView(diff = versionDiff)
            },
            confirmButton = {
                TextButton(onClick = { showDiffDialog = false }) {
                    Text("Close")
                }
            },
        )
    }

    // Version details dialog
    selectedVersion?.let { version ->
        AlertDialog(
            onDismissRequest = onClearSelectedVersion,
            title = { Text("Version ${version.versionNumber} Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Size: ${formatFileSize(version.size)}")
                    Text("Created: ${formatRelativeTime(version.createdAt)}")
                    Text("Created by: ${version.createdBy}")
                    Text("Checksum: ${version.checksum}")
                    if (version.isRestore) {
                        Text("Restored from: v${version.restoredFrom}")
                    }
                    version.comment?.let { comment ->
                        Text("Comment: $comment")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onClearSelectedVersion) {
                    Text("Close")
                }
            },
        )
    }

    // Delete version confirmation dialog
    versionToDelete?.let { version ->
        AlertDialog(
            onDismissRequest = { versionToDelete = null },
            title = { Text("Delete Version ${version.versionNumber}?") },
            text = {
                Text(
                    "Are you sure you want to permanently delete version ${version.versionNumber}? This action cannot be undone.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteVersion(version.id)
                        versionToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { versionToDelete = null }) {
                    Text(strings.actionCancel)
                }
            },
        )
    }

    // Download URL dialog
    downloadUrl?.let { url ->
        AlertDialog(
            onDismissRequest = onClearDownloadUrl,
            title = { Text("Download Ready") },
            text = {
                Column {
                    Text("Your version download is ready.")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "The download will start automatically in your browser.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onClearDownloadUrl) {
                    Text("OK")
                }
            },
        )
    }

    // Diff dialog with clear button
    if (showDiffDialog && versionDiff != null) {
        AlertDialog(
            onDismissRequest = {
                showDiffDialog = false
                onClearDiff()
            },
            title = { Text("Version Comparison") },
            text = {
                Column {
                    Text("Comparing version ${versionDiff.fromVersion} to ${versionDiff.toVersion}")
                    Spacer(Modifier.height(8.dp))
                    Text("Additions: ${versionDiff.additions}")
                    Text("Deletions: ${versionDiff.deletions}")
                    Text("Size change: ${versionDiff.sizeChange} bytes")
                    if (versionDiff.isBinary) {
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
                TextButton(onClick = {
                    showDiffDialog = false
                    onClearDiff()
                }) {
                    Text("Close")
                }
            },
        )
    }

    // Error dialog
    error?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = onClearError,
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = onClearError) {
                    Text("OK")
                }
            },
        )
    }
}
