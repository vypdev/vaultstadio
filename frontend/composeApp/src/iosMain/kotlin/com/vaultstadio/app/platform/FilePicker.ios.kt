/**
 * VaultStadio iOS File Picker Implementation
 *
 * Stub implementations. Full implementation would use UIDocumentPickerViewController.
 */

package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.upload.UploadQueueEntry

/**
 * iOS stub for LargeSelectedFile. readChunk is not supported until native picker is used.
 */
actual class LargeSelectedFile(
    actual val name: String,
    actual val size: Long,
    actual val mimeType: String,
) {
    actual suspend fun readChunk(start: Long, end: Long): ByteArray {
        throw UnsupportedOperationException("Large file picker not yet implemented on iOS")
    }
}

actual suspend fun openFilePicker(
    multiple: Boolean,
    accept: String,
): List<SelectedFile> {
    // TODO: Use UIDocumentPickerViewController
    return emptyList()
}

actual suspend fun openLargeFilePicker(
    multiple: Boolean,
    accept: String,
): List<LargeSelectedFile> {
    return emptyList()
}

actual suspend fun pickLargeFilesForUpload(): List<UploadQueueEntry> = emptyList()

actual fun isFilePickerAvailable(): Boolean = false

actual suspend fun openFolderPicker(): List<FolderFile> {
    return emptyList()
}
