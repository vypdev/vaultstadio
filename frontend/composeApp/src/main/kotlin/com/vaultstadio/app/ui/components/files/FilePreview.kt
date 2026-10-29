package com.vaultstadio.app.ui.components.files

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vaultstadio.app.domain.storage.model.StorageItem

/**
 * Full-screen file preview dialog.
 */
@Composable
fun FilePreviewDialog(
    item: StorageItem,
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isOpen) return

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.9f),
        title = { FilePreviewTitle(item.name, onDownload, onShare, onDismiss) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                FilePreviewContent(item = item)
            }
        },
        confirmButton = { FilePreviewActions(item, onDelete, onDownload) },
    )
}

/**
 * Preview content based on file type.
 */
@Composable
private fun FilePreviewContent(
    item: StorageItem,
    modifier: Modifier = Modifier,
) {
    val mimeType = item.mimeType ?: ""

    when {
        mimeType.startsWith("image/") -> {
            ImagePreview(item = item, modifier = modifier)
        }
        mimeType.startsWith("video/") -> {
            VideoPreview(item = item, modifier = modifier)
        }
        mimeType.startsWith("audio/") -> {
            AudioPreview(item = item, modifier = modifier)
        }
        mimeType == "application/pdf" -> {
            PdfPreview(item = item, modifier = modifier)
        }
        mimeType.startsWith("text/") || isCodeFile(item.name) -> {
            TextPreview(item = item, modifier = modifier)
        }
        else -> {
            GenericPreview(item = item, modifier = modifier)
        }
    }
}
