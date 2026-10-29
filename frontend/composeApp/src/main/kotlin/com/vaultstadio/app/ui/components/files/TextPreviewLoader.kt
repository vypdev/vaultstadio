package com.vaultstadio.app.ui.components.files

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.usecase.DownloadFileUseCase
import kotlinx.coroutines.CancellationException
import kotlin.text.CharacterCodingException

/** Display cap only: the existing domain download API still buffers the complete body. */
// A failing domain port must produce safe UI copy; cancellation and fatal errors escape.
@Suppress("TooGenericExceptionCaught")
internal suspend fun loadTextPreview(download: DownloadFileUseCase, itemId: String): Result<String> = try {
    download(itemId).map { bytes ->
        val limit = 100 * 1024
        if (bytes.size > limit) {
            bytes.decodeToString(endIndex = completeUtf8PrefixEnd(bytes, limit)) +
                "\n\n... (truncated - file too large for preview)"
        } else {
            bytes.decodeToString()
        }
    }
} catch (error: CancellationException) {
    throw error
} catch (_: Exception) {
    Result.Error("PREVIEW_UNAVAILABLE", "Failed to load content. Try again later.")
}

/** Trim only a valid code point crossing the byte boundary; malformed source retains replacement semantics. */
private fun completeUtf8PrefixEnd(bytes: ByteArray, limit: Int): Int {
    if (!bytes[limit].isUtf8Continuation()) return limit
    var start = limit - 1
    while (start > 0 && limit - start < 4 && bytes[start].isUtf8Continuation()) start--
    val width = when (bytes[start].toInt() and 0xFF) {
        in 0xC2..0xDF -> 2
        in 0xE0..0xEF -> 3
        in 0xF0..0xF4 -> 4
        else -> 0
    }
    if (start + width <= limit || start + width > bytes.size) return limit
    return try {
        bytes.decodeToString(startIndex = start, endIndex = start + width, throwOnInvalidSequence = true)
        start
    } catch (_: CharacterCodingException) {
        limit
    }
}

private fun Byte.isUtf8Continuation(): Boolean = toInt() and 0xC0 == 0x80
