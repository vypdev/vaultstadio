package com.vaultstadio.app.feature.upload

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.upload.UploadFailure
import com.vaultstadio.app.domain.upload.UploadQueueEntry
import com.vaultstadio.app.ui.components.dialogs.UploadStatus
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Upload78QueueTest {
    @Test fun transfer78SuccessfulUploadPreservesBytesNameMimeAndParent() = upload78Test {
        add()
        tick()
        val call = repository.calls.single()
        assertContentEquals(byteArrayOf(0, 127, -1), call.bytes)
        assertEquals("archivo ñ.bin", call.name)
        assertEquals("test/binary", call.mime)
        assertEquals("folder78", call.parent)
        assertEquals(UploadStatus.COMPLETED, manager.items.value.single().status)
        assertEquals(1, completions)
        assertFalse(manager.canRetry(manager.items.value.single().id))
    }

    @Test fun transfer78MismatchedSelectedBytesNeverReachTransport() = upload78Test {
        manager.addEntries(listOf(UploadQueueEntry.WithData("truncated", 3, "test/binary", byteArrayOf(1))), null)
        tick()
        assertTrue(repository.calls.isEmpty())
        val item = manager.items.value.single()
        assertEquals(UploadStatus.FAILED, item.status)
        assertEquals(UploadFailure.INVALID_CONTENT, item.failure)
        assertEquals(0, completions)
        assertFalse(manager.canRetry(item.id))
    }

    @Test fun transfer78FailedUploadEmitsNoCompletionAndCanRetrySameSnapshot() = upload78Test {
        repository.transfer = { Result.Error("SERVICE_UNAVAILABLE", "private provider details") }
        add()
        tick()
        val failed = manager.items.value.single()
        assertEquals(UploadStatus.FAILED, failed.status)
        assertEquals(UploadFailure.TRANSFER_FAILED, failed.failure)
        assertEquals(0, completions)
        assertTrue(manager.canRetry(failed.id))
        manager.setUploadDestination("different-current-folder")
        repository.transfer = { Result.Success(upload78Item(it.name, it.parent)) }
        assertTrue(manager.retryUpload(failed.id))
        assertFalse(manager.retryUpload(failed.id))
        tick()
        assertEquals(2, repository.calls.size)
        val (first, retry) = repository.calls
        assertEquals(first.name, retry.name)
        assertEquals(first.mime, retry.mime)
        assertEquals("folder78", retry.parent)
        assertContentEquals(first.bytes, retry.bytes)
        assertEquals(UploadStatus.COMPLETED, manager.items.value.single().status)
        assertEquals(1, completions)
    }

    @Test fun transfer78ThrownTransportFailureIsVisibleAndRetryable() = upload78Test {
        repository.transfer = { error("provider failed") }
        add()
        tick()
        assertEquals(UploadFailure.TRANSFER_FAILED, manager.items.value.single().failure)
        assertEquals(0, completions)
        assertTrue(manager.canRetry(manager.items.value.single().id))
    }

    @Test fun transfer78ProgressIsFiniteClampedAndCannotOverwriteCompletedState() = upload78Test {
        val release = CompletableDeferred<Unit>()
        repository.transfer = {
            release.await()
            Result.Success(upload78Item(it.name))
        }
        add()
        tick()
        val progress = repository.calls.single().progress
        progress(0.4f)
        assertEquals(0.4f, manager.items.value.single().progress)
        progress(Float.NaN)
        progress(Float.POSITIVE_INFINITY)
        assertEquals(0.4f, manager.items.value.single().progress)
        progress(-3f)
        assertEquals(0f, manager.items.value.single().progress)
        progress(7f)
        assertEquals(1f, manager.items.value.single().progress)
        release.complete(Unit)
        tick()
        progress(0.2f)
        assertEquals(UploadStatus.COMPLETED, manager.items.value.single().status)
        assertEquals(1f, manager.items.value.single().progress)
        assertEquals(1, completions)
    }

    @Test fun transfer78DismissReleasesFailedRetryAndPreservesActiveEntry() = upload78Test {
        val release = CompletableDeferred<Unit>()
        repository.transfer = {
            if (it.name == "failed") {
                Result.Error("FAILED", "failure")
            } else {
                release.await()
                Result.Success(upload78Item(it.name))
            }
        }
        add("failed")
        add("active")
        tick()
        val failedId = manager.items.value.first { it.fileName == "failed" }.id
        manager.dismissCompleted()
        assertEquals("active", manager.items.value.single().fileName)
        assertFalse(manager.retryUpload(failedId))
        release.complete(Unit)
        tick()
        assertEquals(1, completions)
    }
}
