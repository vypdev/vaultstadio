/**
 * VaultStadio WASM File Picker Implementation
 *
 * Uses JavaScript interop to access browser file picker.
 * Supports both small files (loaded in memory) and large files (streaming).
 */

package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.upload.ChunkedFileSource
import com.vaultstadio.app.domain.upload.UploadQueueEntry
import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLInputElement
import org.w3c.files.File
import org.w3c.files.FileReader
import org.w3c.files.get
import kotlin.coroutines.resume

/**
 * Implementation of LargeSelectedFile for WASM.
 * Wraps a browser File object for streaming reads.
 */
actual class LargeSelectedFile(
    private val file: File,
) {
    actual val name: String = file.name
    actual val size: Long = file.size.toDouble().toLong()
    actual val mimeType: String = file.type.ifEmpty { "application/octet-stream" }

    /**
     * Read a chunk of the file using File.slice().
     */
    actual suspend fun readChunk(start: Long, end: Long): ByteArray = suspendCancellableCoroutine { cont ->
        val reader = FileReader()

        reader.onload = { _ ->
            try {
                val result = reader.result
                if (result != null) {
                    val arrayBuffer = result.unsafeCast<ArrayBuffer>()
                    val byteArray = arrayBufferToByteArray(arrayBuffer)
                    cont.resume(byteArray)
                } else {
                    cont.resume(ByteArray(0))
                }
            } catch (e: Exception) {
                cont.resume(ByteArray(0))
            }
        }

        reader.onerror = { _ ->
            cont.resume(ByteArray(0))
        }

        // Use slice to read only the specified range
        val blob = file.slice(start.toInt(), end.toInt())
        reader.readAsArrayBuffer(blob)
    }
}

/**
 * Wraps [LargeSelectedFile] as [ChunkedFileSource] for upload queue.
 */
private class LargeFileChunkedSource(private val file: LargeSelectedFile) : ChunkedFileSource {
    override val name: String get() = file.name
    override val size: Long get() = file.size
    override val mimeType: String get() = file.mimeType
    override suspend fun readChunk(start: Long, end: Long): ByteArray = file.readChunk(start, end)
}

actual suspend fun pickLargeFilesForUpload(): List<UploadQueueEntry> {
    val files = openLargeFilePicker(multiple = true, accept = "*/*")
    return files.map { UploadQueueEntry.Chunked(it.name, it.size, it.mimeType, LargeFileChunkedSource(it)) }
}

/**
 * Opens a file picker for large files (streaming mode).
 * Files are NOT loaded into memory.
 */
actual suspend fun openLargeFilePicker(
    multiple: Boolean,
    accept: String,
): List<LargeSelectedFile> = suspendCancellableCoroutine { continuation ->
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    input.multiple = multiple
    input.accept = accept
    input.style.display = "none"

    document.body?.appendChild(input)

    input.onchange = { _ ->
        val files = input.files
        if (files != null && files.length > 0) {
            val selectedFiles = mutableListOf<LargeSelectedFile>()

            for (i in 0 until files.length) {
                val file = files[i]
                if (file != null) {
                    selectedFiles.add(LargeSelectedFile(file))
                }
            }

            document.body?.removeChild(input)
            continuation.resume(selectedFiles)
        } else {
            document.body?.removeChild(input)
            continuation.resume(emptyList())
        }
    }

    // Trigger click to open file dialog
    input.click()

    continuation.invokeOnCancellation {
        try {
            document.body?.removeChild(input)
        } catch (_: Exception) {
            // Ignore if already removed
        }
    }
}

/**
 * Opens a file picker and loads files into memory.
 * Warning: For large files, use openLargeFilePicker instead.
 */
actual suspend fun openFilePicker(
    multiple: Boolean,
    accept: String,
): List<SelectedFile> = suspendCancellableCoroutine { continuation ->
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    input.multiple = multiple
    input.accept = accept
    input.style.display = "none"

    document.body?.appendChild(input)

    input.onchange = { _ ->
        val files = input.files
        if (files != null && files.length > 0) {
            val selectedFiles = mutableListOf<SelectedFile>()
            var pendingReads = files.length

            for (i in 0 until files.length) {
                val file = files[i]
                if (file != null) {
                    // Check file size - if too large, skip loading data
                    val fileSize = file.size.toDouble().toLong()
                    if (fileSize > LARGE_FILE_THRESHOLD) {
                        // For large files, return empty data and show warning
                        selectedFiles.add(
                            SelectedFile(
                                name = file.name,
                                size = fileSize,
                                mimeType = file.type.ifEmpty { "application/octet-stream" },
                                data = ByteArray(0), // Empty - use chunked upload
                            ),
                        )
                        pendingReads--
                        if (pendingReads == 0) {
                            document.body?.removeChild(input)
                            continuation.resume(selectedFiles)
                        }
                    } else {
                        readFileAsBytes(file) { name, size, mimeType, data ->
                            selectedFiles.add(
                                SelectedFile(
                                    name = name,
                                    size = size,
                                    mimeType = mimeType,
                                    data = data,
                                ),
                            )
                            pendingReads--
                            if (pendingReads == 0) {
                                document.body?.removeChild(input)
                                continuation.resume(selectedFiles)
                            }
                        }
                    }
                } else {
                    pendingReads--
                    if (pendingReads == 0) {
                        document.body?.removeChild(input)
                        continuation.resume(selectedFiles)
                    }
                }
            }
        } else {
            document.body?.removeChild(input)
            continuation.resume(emptyList())
        }
    }

    // Trigger click to open file dialog
    input.click()

    continuation.invokeOnCancellation {
        try {
            document.body?.removeChild(input)
        } catch (_: Exception) {
            // Ignore if already removed
        }
    }
}

/**
 * Reads a file as ByteArray.
 */
private fun readFileAsBytes(
    file: File,
    onComplete: (name: String, size: Long, mimeType: String, data: ByteArray) -> Unit,
) {
    val reader = FileReader()
    val fileName = file.name
    val fileSize = file.size.toDouble().toLong()
    val fileMimeType = file.type.ifEmpty { "application/octet-stream" }

    reader.onload = { _ ->
        try {
            val result = reader.result
            if (result != null) {
                val arrayBuffer = result.unsafeCast<ArrayBuffer>()
                val byteArray = arrayBufferToByteArray(arrayBuffer)
                onComplete(fileName, fileSize, fileMimeType, byteArray)
            } else {
                onComplete(fileName, fileSize, fileMimeType, ByteArray(0))
            }
        } catch (e: Exception) {
            onComplete(fileName, fileSize, fileMimeType, ByteArray(0))
        }
    }

    reader.onerror = { _ ->
        onComplete(fileName, fileSize, fileMimeType, ByteArray(0))
    }

    reader.readAsArrayBuffer(file)
}

/**
 * Convert ArrayBuffer to ByteArray.
 */
private fun arrayBufferToByteArray(buffer: ArrayBuffer): ByteArray {
    val int8Array = Int8Array(buffer)
    val length = int8Array.length
    return ByteArray(length) { i -> int8Array[i] }
}

/**
 * File picker is available in browser environment.
 */
actual fun isFilePickerAvailable(): Boolean = true

/**
 * Opens a folder picker for uploading entire directories.
 * Note: Folder upload is not fully supported in Kotlin/WASM due to API limitations.
 * Files are uploaded without preserving folder structure.
 */
actual suspend fun openFolderPicker(): List<FolderFile> {
    // In WASM, we fall back to multiple file selection since webkitdirectory
    // is not easily accessible. The user should select files manually.
    val selectedFiles = openFilePicker(multiple = true)
    return selectedFiles.map { file ->
        FolderFile(
            name = file.name,
            relativePath = file.name, // No folder structure in WASM fallback
            size = file.size,
            mimeType = file.mimeType,
            data = file.data,
        )
    }
}
