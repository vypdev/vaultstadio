@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.vaultstadio.app.feature.upload

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.ChunkedUploadInit
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class Upload78ChunkTest {
    @Test fun transfer78ChunkReaderUsesNegotiatedServerChunkSizeAndExactBytes() = runTest {
        val repo = Upload78ChunkRepository()
        val progress = mutableListOf<Float>()
        assertIs<Result.Success<Unit>>(processor(repo).upload(upload78ChunkEntry(), "parent", progress::add))
        assertEquals(listOf(0, 1, 2), repo.parts.map { it.first })
        assertContentEquals(byteArrayOf(0, 1), repo.parts[0].second)
        assertContentEquals(byteArrayOf(-1, 3), repo.parts[1].second)
        assertContentEquals(byteArrayOf(4), repo.parts[2].second)
        assertEquals(1f, progress.last())
        assertEquals(1, repo.completeCalls)
        assertEquals(0, repo.cancelCalls)
    }

    @Test fun transfer78UnreadableChunkIsRejectedAndOwnedSessionCanceled() = runTest {
        val repo = Upload78ChunkRepository()
        val result = processor(repo).upload(upload78ChunkEntry(truncate = true), null) {}
        assertEquals("UPLOAD_INVALID_CONTENT", assertIs<Result.Error>(result).code)
        assertTrue(repo.parts.isEmpty())
        assertEquals(0, repo.completeCalls)
        assertEquals("session78", repo.canceledSession)
        assertEquals(1, repo.cancelCalls)
    }

    @Test fun transfer78ContradictoryServerChunkCountIsRejectedBeforeReading() = runTest {
        val repo = Upload78ChunkRepository()
        repo.initialized = Result.Success(ChunkedUploadInit("session78", 2, 99))
        val result = processor(repo).upload(upload78ChunkEntry(), null) {}
        assertEquals("UPLOAD_INVALID_CONTENT", assertIs<Result.Error>(result).code)
        assertTrue(repo.parts.isEmpty())
        assertEquals(1, repo.cancelCalls)
    }

    @Test fun transfer78FailedChunkNeverCompletesSessionAndRequestsCleanup() = runTest {
        val repo = Upload78ChunkRepository()
        repo.chunk = { Result.Error("SERVICE_UNAVAILABLE", "failure") }
        assertEquals(
            "SERVICE_UNAVAILABLE",
            assertIs<Result.Error>(processor(repo).upload(upload78ChunkEntry(), null) {}).code,
        )
        assertEquals(1, repo.parts.size)
        assertEquals(0, repo.completeCalls)
        assertEquals(1, repo.cancelCalls)
    }

    @Test fun transfer78FailedCompletionIsNotSuccessAndRequestsCleanup() = runTest {
        val repo = Upload78ChunkRepository()
        repo.finalized = Result.Error("QUOTA_EXCEEDED", "quota")
        assertEquals(
            "QUOTA_EXCEEDED",
            assertIs<Result.Error>(processor(repo).upload(upload78ChunkEntry(), null) {}).code,
        )
        assertEquals(3, repo.parts.size)
        assertEquals(1, repo.completeCalls)
        assertEquals(1, repo.cancelCalls)
    }

    @Test fun transfer78CancellationStopsChunkWorkAndRunsOwnedSessionCleanup() = runTest {
        val repo = Upload78ChunkRepository()
        repo.chunk = { awaitCancellation() }
        val operation = async { processor(repo).upload(upload78ChunkEntry(), null) {} }
        runCurrent()
        assertEquals(1, repo.parts.size)
        operation.cancelAndJoin()
        assertTrue(operation.isCancelled)
        assertEquals(0, repo.completeCalls)
        assertEquals(1, repo.cancelCalls)
    }

    @Test fun transfer78InitializationFailureHasNoSessionToCancel() = runTest {
        val repo = Upload78ChunkRepository()
        repo.initialized = Result.Error("FORBIDDEN", "denied")
        assertEquals("FORBIDDEN", assertIs<Result.Error>(processor(repo).upload(upload78ChunkEntry(), null) {}).code)
        assertTrue(repo.parts.isEmpty())
        assertEquals(0, repo.cancelCalls)
    }

    @Test fun transfer78EmptyChunkedSourceUsesOrdinaryEmptyUpload() = runTest {
        val repo = Upload78ChunkRepository()
        assertIs<Result.Success<Unit>>(processor(repo).upload(upload78ChunkEntry(byteArrayOf()), "empty-parent") {})
        assertContentEquals(byteArrayOf(), repo.calls.single().bytes)
        assertEquals("empty-parent", repo.calls.single().parent)
        assertEquals(0, repo.initCalls)
    }

    private fun TestScope.processor(repo: Upload78ChunkRepository) =
        UploadEntryProcessor(repo, StandardTestDispatcher(testScheduler))
}
