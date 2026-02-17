package com.vaultstadio.app.ui.components.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.i18n.LocalStrings
import com.vaultstadio.app.i18n.clearSelection
import com.vaultstadio.app.i18n.copy
import com.vaultstadio.app.i18n.delete
import com.vaultstadio.app.i18n.itemsSelected
import com.vaultstadio.app.i18n.move
import com.vaultstadio.app.i18n.star

/**
 * Floating toolbar for batch operations on selected items.
 */
@Composable
fun SelectionToolbar(
    selectedCount: Int,
    onDelete: () -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
    showMoveAndCopy: Boolean = true,
    onMove: (() -> Unit)? = null,
    onCopy: (() -> Unit)? = null,
    onStar: (() -> Unit)? = null,
    onDownloadZip: (() -> Unit)? = null,
) {
    val strings = LocalStrings.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, contentDescription = strings.clearSelection)
            }

            Text(
                text = "$selectedCount ${strings.itemsSelected}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )

            if (showMoveAndCopy) {
                if (onMove != null) {
                    IconButton(onClick = onMove) {
                        Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = strings.move)
                    }
                }
                if (onCopy != null) {
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Default.ContentCopy, contentDescription = strings.copy)
                    }
                }
            }

            if (onStar != null) {
                IconButton(onClick = onStar) {
                    Icon(Icons.Default.Star, contentDescription = strings.star)
                }
            }

            if (onDownloadZip != null) {
                IconButton(onClick = onDownloadZip) {
                    Icon(Icons.Default.Download, contentDescription = "Download as ZIP")
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = strings.delete)
            }
        }
    }
}
