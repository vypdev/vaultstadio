package com.vaultstadio.app.ui.components.files

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Get appropriate icon for file type.
 */
internal fun getFileIcon(mimeType: String?, fileName: String): ImageVector {
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
internal fun isCodeFile(fileName: String): Boolean {
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
internal fun formatPreviewFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> {
            "$bytes B"
        }
        bytes < 1024 * 1024 -> {
            "${bytes / 1024} KB"
        }
        bytes < 1024 * 1024 * 1024 -> {
            "${bytes / (1024 * 1024)} MB"
        }
        else -> {
            val gb = bytes.toDouble() / (1024 * 1024 * 1024)
            val rounded = (gb * 100).toLong() / 100.0
            "$rounded GB"
        }
    }
}
