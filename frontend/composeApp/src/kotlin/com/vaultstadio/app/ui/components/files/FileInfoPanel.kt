/**
 * VaultStadio File Info Panel
 *
 * Detailed information panel for selected file/folder.
 */

package com.vaultstadio.app.ui.components.files

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.activity.model.Activity
import com.vaultstadio.app.domain.model.StorageItem
import com.vaultstadio.app.ui.theme.FileColor
import com.vaultstadio.app.ui.theme.FolderColor
import com.vaultstadio.app.utils.formatFileSize
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun FileInfoPanel(
    item: StorageItem,
    itemActivity: List<Activity> = emptyList(),
    isLoadingActivity: Boolean = false,
    isTrashMode: Boolean = false,
    previewUrl: String? = null,
    onClose: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    onStar: () -> Unit,
    onDelete: () -> Unit,
    onRestore: (() -> Unit)? = null,
    onDeletePermanently: (() -> Unit)? = null,
    onVersionHistory: (() -> Unit)? = null,
    onPreview: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.width(320.dp).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }
            HorizontalDivider()

            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (item.isFolder) {
                                FolderColor.copy(alpha = 0.2f)
                            } else {
                                FileColor.copy(alpha = 0.2f)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = getItemIcon(item),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = if (item.isFolder) FolderColor else FileColor,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (item.isFolder) "Folder" else getMimeTypeLabel(item.mimeType),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                QuickActionButton(
                    icon = if (item.isStarred) Icons.Filled.Star else Icons.Filled.StarBorder,
                    label = if (item.isStarred) "Unstar" else "Star",
                    onClick = onStar,
                    tint = if (item.isStarred) Color(0xFFFCD34D) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!item.isFolder) {
                    QuickActionButton(icon = Icons.Filled.Download, label = "Download", onClick = onDownload)
                    if (onPreview != null && previewUrl != null) {
                        QuickActionButton(icon = Icons.Filled.Visibility, label = "Preview", onClick = onPreview)
                    }
                }
                QuickActionButton(icon = Icons.Filled.Share, label = "Share", onClick = onShare)
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Properties",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                PropertyRow(label = "Type", value = if (item.isFolder) "Folder" else "File")
                if (!item.isFolder) {
                    PropertyRow(label = "Size", value = formatFileSize(item.size))
                    PropertyRow(label = "MIME Type", value = item.mimeType ?: "Unknown")
                }
                PropertyRow(
                    label = "Created",
                    value = formatDateTime(item.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())),
                )
                PropertyRow(
                    label = "Modified",
                    value = formatDateTime(item.updatedAt.toLocalDateTime(TimeZone.currentSystemDefault())),
                )
                PropertyRow(label = "Path", value = item.path)
                PropertyRow(label = "Visibility", value = item.visibility.name)
            }

            HorizontalDivider()
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                FileInfoPanelActivitySection(
                    itemActivity = itemActivity,
                    isLoadingActivity = isLoadingActivity,
                )
            }

            HorizontalDivider()
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Actions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                FileInfoPanelActionsSection(
                    isTrashMode = isTrashMode,
                    isFolder = item.isFolder,
                    onRename = onRename,
                    onMove = onMove,
                    onCopy = onCopy,
                    onDelete = onDelete,
                    onRestore = onRestore,
                    onDeletePermanently = onDeletePermanently,
                    onVersionHistory = onVersionHistory,
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
