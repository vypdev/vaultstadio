/**
 * VaultStadio Formatting Utilities Tests
 *
 * Unit tests for formatFileSize, formatRelativeTime, formatDate, getFileIconName,
 * getFileTypeName, isValidEmail, isStrongPassword, truncateText.
 */

package com.vaultstadio.app.utils

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FormattingTest {

    @Test
    fun formatFileSize_bytes() {
        assertEquals("0 B", formatFileSize(0))
        assertEquals("100 B", formatFileSize(100))
        assertEquals("1023 B", formatFileSize(1023))
    }

    @Test
    fun formatFileSize_kilobytes() {
        assertEquals("1 KB", formatFileSize(1024))
        assertEquals("10 KB", formatFileSize(10 * 1024))
    }

    @Test
    fun formatFileSize_megabytes() {
        assertEquals("1 MB", formatFileSize(1024 * 1024))
        assertEquals("2.5 MB", formatFileSize((1024 * 1024 * 2.5).toLong()))
    }

    @Test
    fun formatFileSize_gigabytes() {
        assertEquals("1 GB", formatFileSize(1024L * 1024 * 1024))
    }

    @Test
    fun formatFileSize_terabytes() {
        assertEquals("1 TB", formatFileSize(1024L * 1024 * 1024 * 1024))
    }

    @Test
    fun formatFileSize_roundsToOneDecimalWhenNeeded() {
        // 2.5 MB: 1024*1024*2.5 = 2621440
        val bytes = (1024L * 1024 * 2.5).toLong()
        val result = formatFileSize(bytes)
        assertTrue(result.startsWith("2.5") && result.endsWith("MB"))
    }

    @Test
    fun formatRelativeTime_justNow() {
        val now = kotlin.time.Clock.System.now()
        val result = formatRelativeTime(now)
        assertTrue(result == "Just now" || result.endsWith("ago"), "Expected 'Just now' or 'X ago', got: $result")
    }

    @Test
    fun formatRelativeTime_returnsNonEmpty() {
        val past = Instant.fromEpochMilliseconds(0)
        val result = formatRelativeTime(past)
        assertTrue(result.isNotEmpty())
        assertTrue(
            result == "Just now" ||
                result.endsWith("minute ago") ||
                result.endsWith("minutes ago") ||
                result.endsWith("hour ago") ||
                result.endsWith("hours ago") ||
                result.endsWith("days ago") ||
                result == "Yesterday" ||
                result.endsWith("weeks ago") ||
                result.endsWith("months ago") ||
                result.endsWith("years ago"),
            "Unexpected format: $result",
        )
    }

    @Test
    fun formatDate_withTime() {
        val instant = Instant.parse("2025-06-15T14:30:00Z")
        val result = formatDate(instant, includeTime = true)
        assertTrue(result.contains("2025"))
        assertTrue(result.contains("06") || result.contains("15"))
    }

    @Test
    fun formatDate_withoutTime() {
        val instant = Instant.parse("2025-06-15T14:30:00Z")
        val result = formatDate(instant, includeTime = false)
        assertTrue(result.contains("2025"))
        assertFalse(result.contains("14") && result.contains("30"))
    }

    @Test
    fun getFileIconName_nullMimeType() {
        assertEquals("file", getFileIconName(null))
    }

    @Test
    fun getFileIconName_byCategory() {
        assertEquals("image", getFileIconName("image/jpeg"))
        assertEquals("image", getFileIconName("image/png"))
        assertEquals("video", getFileIconName("video/mp4"))
        assertEquals("audio", getFileIconName("audio/mpeg"))
        assertEquals("text", getFileIconName("text/plain"))
        assertEquals("pdf", getFileIconName("application/pdf"))
        assertEquals("spreadsheet", getFileIconName("application/vnd.ms-excel"))
        assertEquals("document", getFileIconName("application/msword"))
        assertEquals("presentation", getFileIconName("application/vnd.ms-powerpoint"))
        assertEquals("archive", getFileIconName("application/zip"))
        assertEquals("code", getFileIconName("application/json"))
        assertEquals("file", getFileIconName("application/octet-stream"))
    }

    @Test
    fun getFileTypeName_byMimeType() {
        assertEquals("Image", getFileTypeName("image/jpeg", null))
        assertEquals("Video", getFileTypeName("video/mp4", null))
        assertEquals("Audio", getFileTypeName("audio/mpeg", null))
        assertEquals("PDF Document", getFileTypeName("application/pdf", null))
        assertEquals("Spreadsheet", getFileTypeName("application/vnd.spreadsheet", null))
        assertEquals("Document", getFileTypeName("application/vnd.document", null))
        assertEquals("Presentation", getFileTypeName("application/vnd.presentation", null))
        assertEquals("Archive", getFileTypeName("application/zip", null))
    }

    @Test
    fun getFileTypeName_fallbackToExtension() {
        assertEquals("PDF File", getFileTypeName(null, "pdf"))
        assertEquals("TXT File", getFileTypeName(null, "txt"))
    }

    @Test
    fun getFileTypeName_fallbackToFile() {
        assertEquals("File", getFileTypeName(null, null))
    }

    @Test
    fun isValidEmail_valid() {
        assertTrue(isValidEmail("user@example.com"))
        assertTrue(isValidEmail("a+b@test.co"))
        assertTrue(isValidEmail("user.name+tag@domain.org"))
    }

    @Test
    fun isValidEmail_invalid() {
        assertFalse(isValidEmail(""))
        assertFalse(isValidEmail("no-at-sign"))
        assertFalse(isValidEmail("@nodomain.com"))
        assertFalse(isValidEmail("user@"))
    }

    @Test
    fun isStrongPassword() {
        assertFalse(isStrongPassword("short"))
        assertFalse(isStrongPassword(""))
        assertTrue(isStrongPassword("12345678"))
        assertTrue(isStrongPassword("longpassword"))
    }

    @Test
    fun truncateText_short() {
        assertEquals("hi", truncateText("hi", 10))
        assertEquals("hello", truncateText("hello", 5))
    }

    @Test
    fun truncateText_long() {
        assertEquals("hel...", truncateText("hello world", 6))
        assertEquals("a...", truncateText("abcde", 4))
    }
}
