package com.vaultstadio.app.feature.versionhistory

import kotlinx.datetime.Instant
import kotlin.time.Clock

fun formatVersionFileSize(bytes: Long): String {
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
        val rounded = (size * 10).toLong() / 10.0
        val formatted = if (rounded == rounded.toLong().toDouble()) {
            "${rounded.toLong()}"
        } else {
            "$rounded"
        }
        "$formatted ${units[unitIndex]}"
    }
}

fun formatVersionRelativeTime(instant: Instant): String {
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
