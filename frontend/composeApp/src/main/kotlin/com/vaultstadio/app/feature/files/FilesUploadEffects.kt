package com.vaultstadio.app.feature.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.vaultstadio.app.domain.upload.FolderSelectionException
import com.vaultstadio.app.domain.upload.FolderSelectionProblem
import com.vaultstadio.app.domain.upload.UploadQueueEntry
import com.vaultstadio.app.feature.upload.UploadManager
import com.vaultstadio.app.platform.openFilePicker
import com.vaultstadio.app.platform.pickLargeFilesForUpload
import kotlinx.coroutines.CancellationException

@Composable
internal fun FilesUploadEffects(
    action: UploadAction?,
    parentId: String?,
    manager: UploadManager?,
    onHandled: (UploadAction) -> Unit,
    onProblem: suspend (UploadAction, FolderSelectionProblem) -> Unit,
) {
    val folders = LocalFolderUploads.current
    LaunchedEffect(action) {
        val current = action ?: return@LaunchedEffect
        try {
            if (manager == null) return@LaunchedEffect
            when (current) {
                UploadAction.Files -> {
                    val entries = openFilePicker(multiple = true, accept = "*/*")
                        .map { UploadQueueEntry.WithData(it.name, it.size, it.mimeType, it.data) }
                    if (entries.isNotEmpty()) manager.addEntries(entries, parentId)
                }
                UploadAction.Folder -> folders.start(parentId)
                UploadAction.LargeFiles -> {
                    val entries = pickLargeFilesForUpload()
                    if (entries.isNotEmpty()) manager.addEntries(entries, parentId)
                }
            }
        } catch (canceled: CancellationException) {
            throw canceled
        } catch (failure: Exception) {
            onProblem(current, (failure as? FolderSelectionException)?.problem ?: FolderSelectionProblem.READ_FAILED)
        } finally {
            onHandled(current)
        }
    }
}
