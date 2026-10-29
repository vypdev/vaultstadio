package com.vaultstadio.app.ui.components.files

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.usecase.DownloadFileUseCase
import kotlinx.coroutines.CancellationException

/** Display cap only: the existing domain download API still buffers the complete body. */
// A failing domain port must produce safe UI copy; cancellation and fatal errors escape.
@Suppress("TooGenericExceptionCaught")
internal suspend fun loadTextPreview(download: DownloadFileUseCase, itemId: String): Result<String> = try {
    download(itemId).map { bytes ->
        val limit = 100 * 1024
        if (bytes.size > limit) {
            bytes.decodeToString(endIndex = limit) + "\n\n... (truncated - file too large for preview)"
        } else {
            bytes.decodeToString()
        }
    }
} catch (error: CancellationException) {
    throw error
} catch (_: Exception) {
    Result.Error("PREVIEW_UNAVAILABLE", "Failed to load content. Try again later.")
}
