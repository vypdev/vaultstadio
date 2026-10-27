package com.vaultstadio.app.feature.upload

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.upload.FolderUploadEntry
import com.vaultstadio.app.domain.upload.UploadFailure
import com.vaultstadio.app.ui.components.dialogs.UploadStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Upload78FolderTest {
    @Test fun transfer78FolderCreationFailureNeverUploadsIntoPartialParent() = upload78Test {
        repository.folder = { name, parent ->
            if (name == "blocked") Result.Error("FORBIDDEN", "denied") else {
                Result.Success(upload78Item(name, parent, folder = true))
            }
        }
        manager.addFolderEntries(listOf(entry("root/blocked/data.bin")), "destination")
        tick()
        assertEquals(listOf<Pair<String, String?>>("root" to "destination", "blocked" to "root"), repository.folders)
        assertTrue(repository.calls.isEmpty())
        val item = manager.items.value.single()
        assertEquals(UploadStatus.FAILED, item.status)
        assertEquals(UploadFailure.FOLDER_UNAVAILABLE, item.failure)
        assertFalse(manager.canRetry(item.id))
        assertEquals(0, completions)
    }

    @Test fun transfer78FolderSiblingsReuseCreatedParentAndKeepAllPayloads() = upload78Test {
        manager.addFolderEntries(listOf(entry("root/one.bin"), entry("root/two.bin")), "destination")
        tick()
        assertEquals(listOf<Pair<String, String?>>("root" to "destination"), repository.folders)
        assertEquals(listOf("root", "root"), repository.calls.map { it.parent })
        assertEquals(setOf("one.bin", "two.bin"), repository.calls.map { it.name }.toSet())
        assertTrue(manager.items.value.all { it.status == UploadStatus.COMPLETED })
        assertEquals(2, completions)
    }

    private fun entry(path: String) = FolderUploadEntry(
        path.substringAfterLast('/'), path, 1, "application/octet-stream", byteArrayOf(1),
    )
}
