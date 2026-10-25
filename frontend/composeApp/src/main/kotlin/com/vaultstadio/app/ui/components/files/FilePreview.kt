/**
 * VaultStadio File Preview Component
 *
 * Displays file previews based on file type.
 */

package com.vaultstadio.app.ui.components.files

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.vaultstadio.app.config.LocalApiBaseUrl
import com.vaultstadio.app.domain.storage.model.StorageItem
import com.vaultstadio.app.platform.AudioPlayer
import com.vaultstadio.app.platform.PdfViewer
import com.vaultstadio.app.platform.VideoPlayer
import com.vaultstadio.app.utils.formatFileSize
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

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
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                Row {
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Filled.Download, contentDescription = "Download")
                    }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
            }
        },
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
        confirmButton = {
            // Actions at the bottom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // File info
                Column {
                    Text(
                        text = "Size: ${formatFileSize(item.size)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    if (item.mimeType != null) {
                        Text(
                            text = "Type: ${item.mimeType}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }

                Row {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(onClick = onDownload) {
                        Icon(Icons.Filled.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download")
                    }
                }
            }
        },
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

/**
 * Image preview component.
 *
 * Uses Coil3 SubcomposeAsyncImage for async image loading with loading/error states.
 * Loads images from the thumbnail endpoint: GET /api/v1/thumbnails/{itemId}
 */
@Composable
private fun ImagePreview(
    item: StorageItem,
    modifier: Modifier = Modifier,
) {
    val apiBaseUrl = LocalApiBaseUrl.current
    val imageUrl = "$apiBaseUrl/api/v1/thumbnails/${item.id}?size=LARGE"

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = item.name,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentScale = ContentScale.Fit,
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Loading image...",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            error = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Failed to load image",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            success = {
                SubcomposeAsyncImageContent()
            },
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = formatFileSize(item.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
    }
}

/**
 * Video preview component.
 *
 * Uses platform-specific VideoPlayer implementation via expect/actual.
 */
@Composable
private fun VideoPreview(
    item: StorageItem,
    modifier: Modifier = Modifier,
) {
    val apiBaseUrl = LocalApiBaseUrl.current
    val videoUrl = "$apiBaseUrl/api/v1/storage/${item.id}/download"

    Column(
        modifier = modifier.padding(16.dp),
    ) {
        // Header with file info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.VideoFile,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = formatFileSize(item.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Video player
        VideoPlayer(
            url = videoUrl,
            modifier = Modifier.fillMaxSize().weight(1f),
        )
    }
}

/**
 * Audio preview component.
 *
 * Uses platform-specific AudioPlayer implementation via expect/actual.
 */
@Composable
private fun AudioPreview(
    item: StorageItem,
    modifier: Modifier = Modifier,
) {
    val apiBaseUrl = LocalApiBaseUrl.current
    val audioUrl = "$apiBaseUrl/api/v1/storage/${item.id}/download"

    Column(
        modifier = modifier.padding(16.dp),
    ) {
        // Header with file info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AudioFile,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = formatFileSize(item.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Audio player
        AudioPlayer(
            url = audioUrl,
            modifier = Modifier.fillMaxSize().weight(1f),
        )
    }
}

/**
 * PDF preview component.
 *
 * Uses platform-specific PdfViewer implementation via expect/actual.
 */
@Composable
private fun PdfPreview(
    item: StorageItem,
    modifier: Modifier = Modifier,
) {
    val apiBaseUrl = LocalApiBaseUrl.current
    val pdfUrl = "$apiBaseUrl/api/v1/storage/${item.id}/download"

    Column(
        modifier = modifier.padding(16.dp),
    ) {
        // Header with file info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = formatFileSize(item.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // PDF viewer
        PdfViewer(
            url = pdfUrl,
            modifier = Modifier.fillMaxSize().weight(1f),
        )
    }
}

/**
 * Text/Code preview component.
 *
 * Fetches text content from: GET /api/v1/storage/{itemId}/download
 * Limits content to first 100KB to prevent memory issues.
 * For code files, basic syntax highlighting based on file extension.
 */
@Composable
private fun TextPreview(
    item: StorageItem,
    modifier: Modifier = Modifier,
) {
    val apiBaseUrl = LocalApiBaseUrl.current
    var textContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Limit text preview to 100KB
    val maxPreviewSize = 100 * 1024

    LaunchedEffect(item.id) {
        isLoading = true
        error = null
        try {
            val downloadUrl = "$apiBaseUrl/api/v1/storage/${item.id}/download"
            val client = HttpClient()
            try {
                val response = client.get(downloadUrl)
                val bytes = response.body<ByteArray>()
                textContent = if (bytes.size > maxPreviewSize) {
                    bytes.decodeToString(endIndex = maxPreviewSize) + "\n\n... (truncated - file too large for preview)"
                } else {
                    bytes.decodeToString()
                }
            } finally {
                client.close()
            }
        } catch (e: Exception) {
            error = "Failed to load content: ${e.message}"
        }
        isLoading = false
    }

    Column(
        modifier = modifier.padding(16.dp),
    ) {
        // Header with file info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCodeFile(item.name)) "Code File" else "Text File",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Text(
                text = formatFileSize(item.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content area
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loading content...")
                    }
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            textContent != null -> {
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                        )
                        .padding(12.dp),
                ) {
                    SelectionContainer {
                        Text(
                            text = textContent ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Generic file preview.
 */
@Composable
private fun GenericPreview(
    item: StorageItem,
    modifier: Modifier = Modifier,
) {
    val icon = getFileIcon(item.mimeType, item.name)

    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = formatFileSize(item.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        if (item.mimeType != null) {
            Text(
                text = item.mimeType!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No preview available",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
    }
}

/**
 * Get appropriate icon for file type.
 */
private fun getFileIcon(mimeType: String?, fileName: String): ImageVector {
    return when {
        mimeType == null -> Icons.AutoMirrored.Filled.InsertDriveFile
        mimeType.startsWith("image/") -> Icons.Filled.Image
        mimeType.startsWith("video/") -> Icons.Filled.VideoFile
        mimeType.startsWith("audio/") -> Icons.Filled.AudioFile
        mimeType == "application/pdf" -> Icons.Filled.PictureAsPdf
        mimeType.startsWith("text/") -> Icons.Filled.Description
        mimeType.contains("zip") || mimeType.contains("tar") || mimeType.contains("rar") -> Icons.Filled.FolderZip
        mimeType.contains("word") || fileName.endsWith(".doc") || fileName.endsWith(".docx") -> Icons.Filled.Description
        mimeType.contains("excel") || fileName.endsWith(".xls") || fileName.endsWith(".xlsx") -> Icons.Filled.TableChart
        mimeType.contains(
            "powerpoint",
        ) ||
            fileName.endsWith(".ppt") ||
            fileName.endsWith(".pptx") -> Icons.Filled.Slideshow
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}

/**
 * Check if file is a code file.
 */
private fun isCodeFile(fileName: String): Boolean {
    val codeExtensions = setOf(
        "kt", "kts", "java", "py", "js", "ts", "jsx", "tsx",
        "go", "rs", "c", "cpp", "h", "hpp", "cs", "rb", "php",
        "swift", "m", "mm", "scala", "groovy", "sh", "bash",
        "yml", "yaml", "json", "xml", "html", "css", "scss",
        "sql", "md", "gradle", "toml",
    )
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return extension in codeExtensions
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
