/**
 * VaultStadio Formatting Utilities
 */

package com.vaultstadio.app.utils

import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Format file size to human-readable string.
 */
fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"

    val units = listOf("KB", "MB", "GB", "TB", "PB")
    var size = bytes.toDouble()
    var unitIndex = -1

    while (size >= 1024 && unitIndex < units.lastIndex) {
        size /= 1024
        unitIndex++
    }

    return if (unitIndex < 0) {
        "$bytes B"
    } else {
        // Manual formatting since String.format is not available in KMP
        val rounded = (size * 10).toLong() / 10.0
        val formatted = if (rounded == rounded.toLong().toDouble()) {
            "${rounded.toLong()}"
        } else {
            "$rounded"
        }
        "$formatted ${units[unitIndex]}"
    }
}

/**
 * Format date to relative string (e.g., "2 hours ago").
 */
fun formatRelativeTime(instant: Instant): String {
    val now = Clock.System.now()
    val diff = now - instant

    val seconds = diff.inWholeSeconds
    val minutes = diff.inWholeMinutes
    val hours = diff.inWholeHours
    val days = diff.inWholeDays

    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> if (minutes == 1L) "1 minute ago" else "$minutes minutes ago"
        hours < 24 -> if (hours == 1L) "1 hour ago" else "$hours hours ago"
        days < 7 -> if (days == 1L) "Yesterday" else "$days days ago"
        days < 30 -> "${days / 7} weeks ago"
        days < 365 -> "${days / 30} months ago"
        else -> "${days / 365} years ago"
    }
}

/**
 * Format date to local date string.
 */
fun formatDate(instant: Instant, includeTime: Boolean = true): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())

    val date = "${local.year}-${local.monthNumber.toString().padStart(
        2,
        '0',
    )}-${local.dayOfMonth.toString().padStart(2, '0')}"

    return if (includeTime) {
        val time = "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
        "$date $time"
    } else {
        date
    }
}

/**
 * Get file icon name based on MIME type.
 */
fun getFileIconName(mimeType: String?): String {
    return when {
        mimeType == null -> "file"
        mimeType.startsWith("image/") -> "image"
        mimeType.startsWith("video/") -> "video"
        mimeType.startsWith("audio/") -> "audio"
        mimeType.startsWith("text/") -> "text"
        mimeType == "application/pdf" -> "pdf"
        mimeType.contains("spreadsheet") || mimeType.contains("excel") -> "spreadsheet"
        mimeType.contains("document") || mimeType.contains("word") -> "document"
        mimeType.contains("presentation") || mimeType.contains("powerpoint") -> "presentation"
        mimeType.contains("zip") || mimeType.contains("archive") || mimeType.contains("compressed") -> "archive"
        mimeType.contains("javascript") || mimeType.contains("json") || mimeType.contains("xml") -> "code"
        else -> "file"
    }
}

/**
 * Get file type display name.
 */
fun getFileTypeName(mimeType: String?, extension: String?): String {
    return when {
        mimeType?.startsWith("image/") == true -> "Image"
        mimeType?.startsWith("video/") == true -> "Video"
        mimeType?.startsWith("audio/") == true -> "Audio"
        mimeType == "application/pdf" -> "PDF Document"
        mimeType?.contains("spreadsheet") == true -> "Spreadsheet"
        mimeType?.contains("document") == true -> "Document"
        mimeType?.contains("presentation") == true -> "Presentation"
        mimeType?.contains("zip") == true || mimeType?.contains("archive") == true -> "Archive"
        extension != null -> "${extension.uppercase()} File"
        else -> "File"
    }
}

/**
 * Validate email format.
 */
fun isValidEmail(email: String): Boolean {
    val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    return emailRegex.matches(email)
}

/**
 * Validate password strength.
 */
fun isStrongPassword(password: String): Boolean {
    return password.length >= 8
}

/**
 * Truncate text with ellipsis.
 */
fun truncateText(text: String, maxLength: Int): String {
    return if (text.length <= maxLength) {
        text
    } else {
        text.take(maxLength - 3) + "..."
    }
}
