/**
 * VaultStadio Platform File Picker
 *
 * Expect/actual declarations for platform-specific file picking.
 */

package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.upload.UploadQueueEntry

/**
 * Represents a selected file from the file picker.
 * For small files, data is loaded in memory.
 * For large files, use LargeSelectedFile instead.
 */
data class SelectedFile(
    val name: String,
    val size: Long,
    val mimeType: String,
    val data: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as SelectedFile
        return name == other.name && size == other.size && mimeType == other.mimeType
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}

/**
 * Represents a large file that can be read in chunks.
 * Used for files too large to fit in memory.
 */
expect class LargeSelectedFile {
    val name: String
    val size: Long
    val mimeType: String

    /**
     * Read a chunk of the file.
     *
     * @param start Start byte offset (inclusive)
     * @param end End byte offset (exclusive)
     * @return ByteArray containing the chunk data
     */
    suspend fun readChunk(start: Long, end: Long): ByteArray
}

/**
 * Threshold for using chunked upload (100MB).
 * Files larger than this will use streaming upload.
 */
const val LARGE_FILE_THRESHOLD = 100L * 1024L * 1024L

/**
 * Default chunk size for uploads (10MB).
 */
const val DEFAULT_CHUNK_SIZE = 10L * 1024L * 1024L

/**
 * Opens a file picker and returns selected files.
 * Implementation is platform-specific.
 * Warning: This loads files into memory. For large files, use openLargeFilePicker.
 */
expect suspend fun openFilePicker(
    multiple: Boolean = true,
    accept: String = "*/*",
): List<SelectedFile>

/**
 * Opens a file picker for large files (streaming mode).
 * Files are not loaded into memory.
 */
expect suspend fun openLargeFilePicker(
    multiple: Boolean = true,
    accept: String = "*/*",
): List<LargeSelectedFile>

/**
 * Check if file picker is available on this platform.
 */
expect fun isFilePickerAvailable(): Boolean

/**
 * Picks one or more large files (streaming) and returns them as upload queue entries for chunked upload.
 */
expect suspend fun pickLargeFilesForUpload(): List<UploadQueueEntry>

/**
 * Opens a folder picker for uploading entire directories.
 * Returns files with their relative paths within the folder.
 */
expect suspend fun openFolderPicker(): List<FolderFile>

/**
 * Represents a file from a folder upload with its relative path.
 */
data class FolderFile(
    val name: String,
    val relativePath: String,
    val size: Long,
    val mimeType: String,
    val data: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as FolderFile
        return name == other.name && relativePath == other.relativePath && size == other.size
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + relativePath.hashCode()
        result = 31 * result + size.hashCode()
        return result
    }
}
