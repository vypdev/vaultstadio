package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.upload.FolderSelectionException
import com.vaultstadio.app.domain.upload.FolderSelectionProblem
import com.vaultstadio.app.feature.upload.readFolderSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Path

/** Chooser owns EDT; only approved metadata preparation and bounded reads enter IO. */
internal suspend fun selectDesktopFolderFiles(
    choose: suspend () -> File? = { chooseDesktopFolder() },
    read: suspend (File) -> List<FolderFile> = { readDesktopFolderFiles(it) },
): List<FolderFile> {
    val selected = choose() ?: return emptyList()
    return withContext(Dispatchers.IO) { read(selected) }
}

internal suspend fun readDesktopFolderFiles(
    selectedDir: File,
    readFile: (Path, Long) -> ByteArray = ::readDesktopFolderBytes,
): List<FolderFile> = try {
    val records = collectDesktopFolderMetadata(selectedDir.toPath(), currentCoroutineContext())
    val sources = records.map { DesktopFolderSource(it, readFile) }
    readFolderSelection(sources, requireRoot = false)
        .map { FolderFile(it.name, it.relativePath, it.size, it.mimeType, it.data) }
} catch (_: IOException) {
    throw FolderSelectionException(FolderSelectionProblem.READ_FAILED)
} catch (_: SecurityException) {
    throw FolderSelectionException(FolderSelectionProblem.READ_FAILED)
}
