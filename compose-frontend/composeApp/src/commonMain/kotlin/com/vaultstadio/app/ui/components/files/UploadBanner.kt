/**
 * Bottom-right upload progress banner (Google Drive style).
 * Shows current uploads with progress; can be minimized.
 */

package com.vaultstadio.app.ui.components.files

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.ui.components.dialogs.UploadItem
import com.vaultstadio.app.ui.components.dialogs.UploadStatus
import com.vaultstadio.app.utils.formatFileSize

@Composable
fun UploadBanner(
    uploadItems: List<UploadItem>,
    isMinimized: Boolean,
    onSetMinimized: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onCancelItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = uploadItems.isNotEmpty(),
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier,
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Uploads",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { onSetMinimized(!isMinimized) },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = if (isMinimized) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                            contentDescription = if (isMinimized) "Expand" else "Minimize",
                        )
                    }
                    val allDone = uploadItems.all {
                        it.status == UploadStatus.COMPLETED || it.status == UploadStatus.FAILED
                    }
                    if (allDone) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Dismiss")
                        }
                    }
                }

                val inProgress = uploadItems.count { it.status == UploadStatus.UPLOADING }
                val completed = uploadItems.count { it.status == UploadStatus.COMPLETED }
                val failed = uploadItems.count { it.status == UploadStatus.FAILED }
                val totalProgress = if (uploadItems.isEmpty()) {
                    0f
                } else {
                    uploadItems.sumOf { it.progress.toDouble() }.toFloat() / uploadItems.size
                }

                LinearProgressIndicator(
                    progress = { totalProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                )
                Text(
                    text = buildString {
                        append("$completed / ${uploadItems.size} completed")
                        if (inProgress > 0) append(" • $inProgress uploading")
                        if (failed > 0) append(" • $failed failed")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (!isMinimized) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.heightIn(max = 200.dp),
                    ) {
                        items(uploadItems) { item ->
                            UploadBannerItemRow(
                                item = item,
                                onCancel = { onCancelItem(item.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadBannerItemRow(
    item: UploadItem,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            when (item.status) {
                UploadStatus.PENDING ->
                    Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null, Modifier.size(20.dp))
                UploadStatus.UPLOADING ->
                    CircularProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                UploadStatus.COMPLETED ->
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                UploadStatus.FAILED ->
                    Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.fileName,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatFileSize(item.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (item.status == UploadStatus.UPLOADING) {
            IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
            }
        }
    }
}
