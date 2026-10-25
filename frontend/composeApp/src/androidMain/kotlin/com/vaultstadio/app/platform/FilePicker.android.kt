/**
 * VaultStadio Android File Picker Implementation
 *
 * Uses Android's Storage Access Framework.
 */

package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.upload.ChunkedFileSource
import com.vaultstadio.app.domain.upload.UploadQueueEntry
import java.io.InputStream

/**
 * Large file wrapper for Android.
 */
actual class LargeSelectedFile(
    private val inputStream: InputStream,
    actual val name: String,
    actual val size: Long,
    actual val mimeType: String,
) {
    actual suspend fun readChunk(start: Long, end: Long): ByteArray {
        inputStream.skip(start)
        val size = (end - start).toInt()
        val buffer = ByteArray(size)
        val bytesRead = inputStream.read(buffer)
        return if (bytesRead < size) buffer.copyOf(bytesRead) else buffer
    }
}

/**
 * Opens a file picker and returns selected files.
 */
actual suspend fun openFilePicker(
    multiple: Boolean,
    accept: String,
): List<SelectedFile> {
    // Android implementation would use Activity Result API
    return emptyList()
}

/**
 * Opens a file picker for large files.
 */
actual suspend fun openLargeFilePicker(
    multiple: Boolean,
    accept: String,
): List<LargeSelectedFile> {
    // Android implementation would use Activity Result API
    return emptyList()
}

actual suspend fun pickLargeFilesForUpload(): List<UploadQueueEntry> {
    val files = openLargeFilePicker(multiple = true, accept = "*/*")
    return files.map { f ->
        object : ChunkedFileSource {
            override val name: String get() = f.name
            override val size: Long get() = f.size
            override val mimeType: String get() = f.mimeType
            override suspend fun readChunk(start: Long, end: Long): ByteArray = f.readChunk(start, end)
        }.let { source -> UploadQueueEntry.Chunked(f.name, f.size, f.mimeType, source) }
    }
}

/**
 * Check if file picker is available.
 */
actual fun isFilePickerAvailable(): Boolean = true

/**
 * Opens a folder picker.
 */
actual suspend fun openFolderPicker(): List<FolderFile> {
    // Android implementation would use Storage Access Framework
    return emptyList()
}
