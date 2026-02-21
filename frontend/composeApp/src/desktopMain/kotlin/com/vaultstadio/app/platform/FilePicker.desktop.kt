@file:Suppress("MatchingDeclarationName")
/**
 * VaultStadio Desktop File Picker Implementation
 *
 * Uses AWT FileDialog for desktop platforms.
 * Supports both small files (loaded in memory) and large files (streaming).
 */

package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.upload.ChunkedFileSource
import com.vaultstadio.app.domain.upload.UploadQueueEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.Files

/**
 * Implementation of LargeSelectedFile for Desktop.
 * Wraps a Java File object for streaming reads.
 */
actual class LargeSelectedFile(
    private val file: File,
) {
    actual val name: String = file.name
    actual val size: Long = file.length()
    actual val mimeType: String = Files.probeContentType(file.toPath()) ?: "application/octet-stream"

    /**
     * Read a chunk of the file using RandomAccessFile.
     */
    actual suspend fun readChunk(start: Long, end: Long): ByteArray = withContext(Dispatchers.IO) {
        val chunkSize = (end - start).toInt()
        val buffer = ByteArray(chunkSize)

        RandomAccessFile(file, "r").use { raf ->
            raf.seek(start)
            val bytesRead = raf.read(buffer)
            if (bytesRead < chunkSize) {
                buffer.copyOf(bytesRead)
            } else {
                buffer
            }
        }
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
): List<LargeSelectedFile> = withContext(Dispatchers.IO) {
    val dialog = FileDialog(null as Frame?, "Select Files", FileDialog.LOAD)
    dialog.isMultipleMode = multiple
    dialog.isVisible = true

    val files = dialog.files
    if (files.isNullOrEmpty()) {
        return@withContext emptyList()
    }

    files.map { file -> LargeSelectedFile(file) }
}

/**
 * Opens a file picker using AWT FileDialog.
 * Warning: For large files, use openLargeFilePicker instead.
 */
actual suspend fun openFilePicker(
    multiple: Boolean,
    accept: String,
): List<SelectedFile> = withContext(Dispatchers.IO) {
    val dialog = FileDialog(null as Frame?, "Select Files", FileDialog.LOAD)
    dialog.isMultipleMode = multiple
    dialog.isVisible = true

    val files = dialog.files
    if (files.isNullOrEmpty()) {
        return@withContext emptyList()
    }

        files.mapNotNull { file ->
            try {
                // Check file size - if too large, skip loading data
                if (file.length() > LARGE_FILE_THRESHOLD) {
                    val mimeType = Files.probeContentType(file.toPath()) ?: "application/octet-stream"
                    SelectedFile(
                        name = file.name,
                        size = file.length(),
                        mimeType = mimeType,
                        data = ByteArray(0), // Empty - use chunked upload
                    )
                } else {
                    val bytes = Files.readAllBytes(file.toPath())
                    val mimeType = Files.probeContentType(file.toPath()) ?: "application/octet-stream"
                    SelectedFile(
                        name = file.name,
                        size = file.length(),
                        mimeType = mimeType,
                        data = bytes,
                    )
                }
            } catch (_: IOException) {
                // Skip unreadable file
                null
            }
        }
}

/**
 * File picker is available on desktop.
 */
actual fun isFilePickerAvailable(): Boolean = true

/**
 * Adds a single file to [out] if readable and under size limit; returns true if added.
 */
private fun addFileIfReadable(file: File, basePathLength: Int, out: MutableList<FolderFile>): Boolean {
    if (file.length() > LARGE_FILE_THRESHOLD) return false
    return try {
        val relativePath = file.absolutePath.substring(basePathLength)
        val bytes = Files.readAllBytes(file.toPath())
        val mimeType = Files.probeContentType(file.toPath()) ?: "application/octet-stream"
        out.add(
            FolderFile(
                name = file.name,
                relativePath = relativePath,
                size = file.length(),
                mimeType = mimeType,
                data = bytes,
            ),
        )
        true
    } catch (_: IOException) {
        false
    }
}

/**
 * Collects all files under [dir] into [out], with [basePathLength] for relative paths.
 * Skips files larger than [LARGE_FILE_THRESHOLD]; skips unreadable files.
 */
private fun collectFolderFiles(dir: File, basePathLength: Int, out: MutableList<FolderFile>) {
    for (file in dir.listFiles().orEmpty()) {
        if (file.isDirectory) {
            collectFolderFiles(file, basePathLength, out)
        } else {
            addFileIfReadable(file, basePathLength, out)
        }
    }
}

/**
 * Opens a folder picker for uploading entire directories.
 */
actual suspend fun openFolderPicker(): List<FolderFile> = withContext(Dispatchers.IO) {
    val dialog = javax.swing.JFileChooser()
    dialog.fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
    dialog.dialogTitle = "Select Folder to Upload"

    val result = dialog.showOpenDialog(null)
    if (result != javax.swing.JFileChooser.APPROVE_OPTION) {
        return@withContext emptyList()
    }

    val selectedDir = dialog.selectedFile
    if (!selectedDir.isDirectory) {
        return@withContext emptyList()
    }

    val basePathLength = selectedDir.absolutePath.length + 1
    val allFiles = mutableListOf<FolderFile>()
    collectFolderFiles(selectedDir, basePathLength, allFiles)
    allFiles
}
