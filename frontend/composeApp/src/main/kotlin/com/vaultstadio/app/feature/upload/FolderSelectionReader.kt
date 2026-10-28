package com.vaultstadio.app.feature.upload

import com.vaultstadio.app.domain.upload.FolderFileMetadata
import com.vaultstadio.app.domain.upload.FolderFileSource
import com.vaultstadio.app.domain.upload.FolderSelectionException
import com.vaultstadio.app.domain.upload.FolderSelectionProblem
import com.vaultstadio.app.domain.upload.FolderUploadEntry
import com.vaultstadio.app.domain.upload.folderSelectionProblem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Sequential preparation; a failed/canceled read never publishes a partial folder selection. */
internal suspend fun readFolderSelection(
    sources: List<FolderFileSource>,
    requireRoot: Boolean = true,
): List<FolderUploadEntry> {
    val snapshot: List<Pair<FolderFileSource, FolderFileMetadata>> = sources.map { it to it.metadata.copy() }
    folderSelectionProblem(snapshot.map { it.second }, requireRoot)?.let { throw FolderSelectionException(it) }
    val result = mutableListOf<FolderUploadEntry>()
    for ((source, metadata) in snapshot) {
        currentCoroutineContext().ensureActive()
        val bytes = try {
            source.readBytes()
        } catch (canceled: CancellationException) {
            throw canceled
        } catch (failure: Exception) {
            throw FolderSelectionException(FolderSelectionProblem.READ_FAILED)
        }
        currentCoroutineContext().ensureActive()
        if (bytes.size.toLong() != metadata.size) throw FolderSelectionException(FolderSelectionProblem.READ_FAILED)
        result += FolderUploadEntry(metadata.name, metadata.relativePath, metadata.size, metadata.mimeType, bytes)
    }
    currentCoroutineContext().ensureActive()
    return result
}
