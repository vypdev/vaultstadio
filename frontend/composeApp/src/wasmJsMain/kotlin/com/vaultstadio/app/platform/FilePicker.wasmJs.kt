package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.upload.ChunkedFileSource
import com.vaultstadio.app.domain.upload.FolderSelectionException
import com.vaultstadio.app.domain.upload.FolderSelectionProblem
import com.vaultstadio.app.domain.upload.UploadQueueEntry
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.w3c.files.File

actual class LargeSelectedFile(private val file: File) {
    actual val name: String = file.name
    actual val size: Long = browserFileSize(file)
    actual val mimeType: String = file.type.ifEmpty { "application/octet-stream" }

    actual suspend fun readChunk(start: Long, end: Long): ByteArray {
        if (start < 0 || end < start || end > size || end - start > Int.MAX_VALUE.toLong()) {
            throw FolderSelectionException(FolderSelectionProblem.READ_FAILED)
        }
        val bytes = readBrowserBlob(browserSlice(file, start, end), end - start)
        if (bytes.size.toLong() != end - start) throw FolderSelectionException(FolderSelectionProblem.READ_FAILED)
        return bytes
    }
}

private class LargeFileChunkedSource(private val file: LargeSelectedFile) : ChunkedFileSource {
    override val name: String get() = file.name
    override val size: Long get() = file.size
    override val mimeType: String get() = file.mimeType
    override suspend fun readChunk(start: Long, end: Long): ByteArray = file.readChunk(start, end)
}

actual suspend fun pickLargeFilesForUpload(): List<UploadQueueEntry> =
    openLargeFilePicker(multiple = true, accept = "*/*")
        .map { UploadQueueEntry.Chunked(it.name, it.size, it.mimeType, LargeFileChunkedSource(it)) }

actual suspend fun openLargeFilePicker(multiple: Boolean, accept: String): List<LargeSelectedFile> =
    selectBrowserFiles(multiple, accept).map { LargeSelectedFile(it) }

actual suspend fun openFilePicker(multiple: Boolean, accept: String): List<SelectedFile> =
    selectBrowserFiles(multiple, accept).map { file ->
        currentCoroutineContext().ensureActive()
        val size = browserFileSize(file)
        val bytes = if (size >
            LARGE_FILE_THRESHOLD
        ) {
            byteArrayOf()
        } else {
            readBrowserBlob(browserSlice(file, 0, size + 1), size + 1)
        }
        currentCoroutineContext().ensureActive()
        if (size <= LARGE_FILE_THRESHOLD &&
            bytes.size.toLong() != size
        ) {
            throw FolderSelectionException(FolderSelectionProblem.READ_FAILED)
        }
        SelectedFile(file.name, size, file.type.ifEmpty { "application/octet-stream" }, bytes)
    }

actual fun isFilePickerAvailable(): Boolean = true
actual fun isFolderPickerAvailable(): Boolean = browserDirectoryPickerAvailable()
actual suspend fun openFolderPicker(): List<FolderFile> = selectBrowserFolder()
