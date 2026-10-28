package com.vaultstadio.app.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files

/** Chooser owns EDT; only approved enumeration and content reads enter the IO dispatcher. */
internal suspend fun selectDesktopFolderFiles(
    choose: suspend () -> File? = { chooseDesktopFolder() },
    read: (File) -> List<FolderFile> = ::readDesktopFolderFiles,
): List<FolderFile> {
    val selected = choose() ?: return emptyList()
    return withContext(Dispatchers.IO) { read(selected) }
}

internal fun readDesktopFolderFiles(selectedDir: File): List<FolderFile> {
    if (!selectedDir.isDirectory) return emptyList()
    val basePathLength = selectedDir.absolutePath.length + 1
    val allFiles = mutableListOf<FolderFile>()
    collectFolderFiles(selectedDir, basePathLength, allFiles)
    return allFiles
}

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
