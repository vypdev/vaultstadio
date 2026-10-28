@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.upload.FOLDER_SELECTION_MAX_ENTRIES
import com.vaultstadio.app.domain.upload.FolderFileMetadata
import com.vaultstadio.app.domain.upload.FolderFileSource
import com.vaultstadio.app.feature.upload.readFolderSelection
import org.w3c.files.File

internal suspend fun selectBrowserFolder(): List<FolderFile> {
    val files =
        selectBrowserFiles(
            multiple = true,
            accept = "*/*",
            directory = true,
            maximumFiles = FOLDER_SELECTION_MAX_ENTRIES,
        )
    return readFolderSelection(files.map { BrowserFolderFile(it) })
        .map { FolderFile(it.name, it.relativePath, it.size, it.mimeType, it.data) }
}

private class BrowserFolderFile(private val file: File) : FolderFileSource {
    override val metadata = FolderFileMetadata(
        file.name,
        browserRelativePath(file),
        browserFileSize(file),
        file.type.ifEmpty { "application/octet-stream" },
    )
    override suspend fun readBytes(): ByteArray =
        readBrowserBlob(browserSlice(file, 0, metadata.size + 1), metadata.size + 1)
}

private fun browserRelativePath(file: File): String = js("file.webkitRelativePath || ''")
