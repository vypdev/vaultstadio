package com.vaultstadio.app.feature.upload

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.upload.UploadFailure
import com.vaultstadio.app.ui.components.dialogs.UploadStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Upload78CancellationTest {
    @Test fun transfer78CancelStopsOneActiveChildWithoutStoppingAnotherUpload() = upload78Test {
        val release = CompletableDeferred<Unit>()
        val canceled = mutableListOf<String>()
        repository.transfer = {
            try {
                release.await()
                Result.Success(upload78Item(it.name))
            } finally {
                canceled += it.name
            }
        }
        add("cancel-me")
        add("keep-me")
        tick()
        manager.cancelUpload(manager.items.value.first().id)
        tick()
        assertEquals(listOf("cancel-me"), canceled)
        assertEquals(UploadStatus.CANCELED, manager.items.value.first().status)
        assertEquals(UploadFailure.CANCELED, manager.items.value.first().failure)
        assertEquals(UploadStatus.UPLOADING, manager.items.value.last().status)
        assertEquals(0, completions)
        release.complete(Unit)
        tick()
        assertEquals(UploadStatus.COMPLETED, manager.items.value.last().status)
        assertEquals(1, completions)
    }

    @Test fun transfer78CancelPendingEntryNeverStartsItsNetworkRequest() = upload78Test {
        repository.transfer = { awaitCancellation() }
        repeat(4) { add("queued-$it") }
        tick()
        assertEquals(3, repository.calls.size)
        val pending = manager.items.value.last()
        manager.cancelUpload(pending.id)
        manager.cancelUpload(manager.items.value.first().id)
        tick()
        assertEquals(3, repository.calls.size)
        assertEquals(UploadStatus.CANCELED, manager.items.value.last().status)
        assertEquals(0, completions)
    }

    @Test fun transfer78CanceledAttemptCannotChangeRetriedProgress() = upload78Test {
        repository.transfer = { awaitCancellation() }
        add()
        tick()
        val id = manager.items.value.single().id
        val stale = repository.calls.single().progress
        manager.cancelUpload(id)
        assertFalse(manager.canRetry(id))
        tick()
        assertTrue(manager.canRetry(id))
        assertTrue(manager.retryUpload(id))
        tick()
        repository.calls.last().progress(0.6f)
        stale(0.9f)
        assertEquals(0.6f, manager.items.value.single().progress)
        assertEquals(UploadStatus.UPLOADING, manager.items.value.single().status)
        assertEquals(0, completions)
    }

    @Test fun transfer78ClearCancelsOwnedWorkAndRejectsNewEntriesOrRetry() = upload78Test {
        var finalized = false
        repository.transfer = { try { awaitCancellation() } finally { finalized = true } }
        add()
        tick()
        val id = manager.items.value.single().id
        manager.clear()
        manager.clear()
        add("after-close")
        tick()
        assertTrue(finalized)
        assertEquals(1, repository.calls.size)
        assertEquals(1, manager.items.value.size)
        assertFalse(manager.retryUpload(id))
        assertEquals(0, completions)
    }
}
