package com.vaultstadio.app.feature.upload

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.ItemType
import com.vaultstadio.app.domain.storage.model.StorageItem
import com.vaultstadio.app.domain.storage.model.Visibility
import com.vaultstadio.app.domain.upload.UploadQueueEntry
import com.vaultstadio.app.ui.components.dialogs.UploadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class Upload78EmptyTest {
    @Test fun transfer78ZeroByteSelectionUploadsRatherThanSilentlyFailing() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        var calls = 0
        val repository = object : StubStorageRepository() {
            override suspend fun uploadFile(
                fileName: String,
                fileData: ByteArray,
                mimeType: String,
                parentId: String?,
                onProgress: (Float) -> Unit,
            ): Result<StorageItem> {
                calls++
                return Result.Success(
                    StorageItem(
                        "empty78", fileName, "/$fileName", ItemType.FILE, parentId, 0,
                        mimeType, Visibility.PRIVATE, false, false,
                        Instant.fromEpochMilliseconds(
                            0,
                        ),
                        Instant.fromEpochMilliseconds(0),
                    ),
                )
            }
        }
        val manager = UploadManager(repository)
        try {
            manager.addEntries(
                listOf(UploadQueueEntry.WithData("empty78.txt", 0, "text/plain", byteArrayOf())),
                "folder78",
            )
            val terminal = withContext(Dispatchers.Default) {
                withTimeout(5000) {
                    manager.items.first {
                        it.singleOrNull()?.status in
                            listOf(UploadStatus.FAILED, UploadStatus.COMPLETED)
                    }.single()
                }
            }
            assertEquals(UploadStatus.COMPLETED, terminal.status)
            assertEquals(1, calls)
        } finally {
            manager.clear()
            Dispatchers.resetMain()
        }
    }
}
