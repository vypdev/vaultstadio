/**
 * Reusable parts for FileInfoPanel: property rows, action buttons, activity items, formatting.
 * Extracted to keep FileInfoPanel.kt under the component line limit.
 */

package com.vaultstadio.app.ui.components.files

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.domain.model.Activity
import com.vaultstadio.app.utils.formatRelativeTime
import kotlinx.datetime.LocalDateTime

@Composable
internal fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(imageVector = icon, contentDescription = label, tint = tint)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun PropertyRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

@Composable
internal fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
) {
    val color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}

internal fun getMimeTypeLabel(mimeType: String?): String = when {
    mimeType == null -> "File"
    mimeType.startsWith("image/") -> "Image"
    mimeType.startsWith("video/") -> "Video"
    mimeType.startsWith("audio/") -> "Audio"
    mimeType == "application/pdf" -> "PDF Document"
    mimeType.contains("zip") -> "Archive"
    mimeType.contains("text") -> "Text File"
    mimeType.contains("json") -> "JSON File"
    mimeType.contains("xml") -> "XML File"
    else -> "File"
}

internal fun formatDateTime(dateTime: LocalDateTime): String {
    val month = dateTime.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    return "$month ${dateTime.dayOfMonth}, ${dateTime.year} at ${
        dateTime.hour.toString().padStart(2, '0')
    }:${dateTime.minute.toString().padStart(2, '0')}"
}

@Composable
internal fun ActivityItem(activity: Activity) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.type.replace("_", " ").replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = formatRelativeTime(activity.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun FileInfoPanelActivitySection(
    itemActivity: List<Activity>,
    isLoadingActivity: Boolean,
) {
    if (isLoadingActivity) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
    } else if (itemActivity.isEmpty()) {
        Text(
            text = "No recent activity",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        itemActivity.take(5).forEach { activity ->
            ActivityItem(activity = activity)
        }
    }
}

@Composable
internal fun FileInfoPanelActionsSection(
    isTrashMode: Boolean,
    isFolder: Boolean,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onRestore: (() -> Unit)?,
    onDeletePermanently: (() -> Unit)?,
    onVersionHistory: (() -> Unit)?,
) {
    if (isTrashMode) {
        onRestore?.let {
            ActionButton(icon = Icons.Filled.RestoreFromTrash, label = "Restore", onClick = it)
        }
        onDeletePermanently?.let {
            ActionButton(
                icon = Icons.Filled.DeleteForever,
                label = "Delete permanently",
                onClick = it,
                isDestructive = true,
            )
        }
    } else {
        ActionButton(icon = Icons.Filled.Edit, label = "Rename", onClick = onRename)
        ActionButton(icon = Icons.AutoMirrored.Filled.DriveFileMove, label = "Move to...", onClick = onMove)
        ActionButton(icon = Icons.Filled.ContentCopy, label = "Make a copy", onClick = onCopy)
        if (!isFolder && onVersionHistory != null) {
            ActionButton(icon = Icons.Filled.History, label = "Version history", onClick = onVersionHistory)
        }
        Spacer(modifier = Modifier.height(8.dp))
        ActionButton(
            icon = Icons.Filled.Delete,
            label = "Move to trash",
            onClick = onDelete,
            isDestructive = true,
        )
    }
}
