package com.vaultstadio.app.domain.upload

const val FOLDER_SELECTION_MAX_BYTES = 32L * 1024 * 1024
const val FOLDER_SELECTION_MAX_ENTRIES = 256
const val FOLDER_SELECTION_MAX_DEPTH = 16

data class FolderFileMetadata(val name: String, val relativePath: String, val size: Long, val mimeType: String)

interface FolderFileSource {
    val metadata: FolderFileMetadata
    suspend fun readBytes(): ByteArray
}

enum class FolderSelectionProblem {
    UNSUPPORTED,
    INVALID_PATH,
    CONFLICTING_PATHS,
    TOO_MANY_ENTRIES,
    TOO_LARGE,
    TOO_DEEP,
    READ_FAILED,
}

class FolderSelectionException(val problem: FolderSelectionProblem) : Exception("File selection failed: $problem")

/** Validate the whole metadata snapshot before any body read or remote mutation. */
fun folderSelectionProblem(entries: List<FolderFileMetadata>, requireRoot: Boolean = true): FolderSelectionProblem? {
    if (entries.size > FOLDER_SELECTION_MAX_ENTRIES) return FolderSelectionProblem.TOO_MANY_ENTRIES
    var bytes = 0L
    val files = mutableSetOf<String>()
    val directories = mutableSetOf<String>()
    for (entry in entries) {
        if (entry.size < 0 || entry.size > FOLDER_SELECTION_MAX_BYTES - bytes) return FolderSelectionProblem.TOO_LARGE
        bytes += entry.size
        if (entry.relativePath.length > FOLDER_SELECTION_MAX_DEPTH * 256 - 1) return FolderSelectionProblem.INVALID_PATH
        val segments = entry.relativePath.split('/')
        if ((requireRoot && segments.size < 2) ||
            segments.last() != entry.name ||
            segments.any { !safeFolderSegment(it) }
        ) {
            return FolderSelectionProblem.INVALID_PATH
        }
        if (segments.size > FOLDER_SELECTION_MAX_DEPTH) return FolderSelectionProblem.TOO_DEEP
        if (!files.add(entry.relativePath)) return FolderSelectionProblem.CONFLICTING_PATHS
        for (end in 1 until segments.size) directories += segments.take(end).joinToString("/")
        if (files.size + directories.size > FOLDER_SELECTION_MAX_ENTRIES) return FolderSelectionProblem.TOO_MANY_ENTRIES
    }
    return if (files.any { it in directories }) FolderSelectionProblem.CONFLICTING_PATHS else null
}

private fun safeFolderSegment(segment: String): Boolean =
    segment.isNotBlank() &&
        segment.length <= 255 &&
        segment != "." &&
        segment != ".." &&
        segment.none { it == '\\' || it == ':' || it.isISOControl() }
