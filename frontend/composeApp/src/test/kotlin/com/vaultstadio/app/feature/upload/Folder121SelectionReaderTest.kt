@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.vaultstadio.app.feature.upload

import com.vaultstadio.app.domain.upload.FolderFileMetadata
import com.vaultstadio.app.domain.upload.FolderFileSource
import com.vaultstadio.app.domain.upload.FolderSelectionException
import com.vaultstadio.app.domain.upload.FolderSelectionProblem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class Folder121SelectionReaderTest {
    @Test fun folder121ReaderPreservesBinaryNamePathAndMime() = runTest {
        val bytes = byteArrayOf(0, -1, 127, -128)
        val file = source("raíz/日本語/ñ.bin", bytes)
        val actual = readFolderSelection(listOf(file)).single()
        assertEquals(file.metadata.name, actual.name)
        assertEquals(file.metadata.relativePath, actual.relativePath)
        assertEquals(file.metadata.mimeType, actual.mimeType)
        assertContentEquals(bytes, actual.data)
    }

    @Test fun folder121ReaderIncludesActualEmptyFile() = runTest {
        val actual = readFolderSelection(listOf(source("root/empty", byteArrayOf()))).single()
        assertEquals(0L, actual.size)
        assertContentEquals(byteArrayOf(), actual.data)
    }

    @Test fun folder121ReaderKeepsInputOrderAndReadsSequentially() = runTest {
        val release = CompletableDeferred<Unit>()
        val calls = mutableListOf<String>()
        val first = source("r/z", byteArrayOf(1)) {
            calls += "first"
            release.await()
            byteArrayOf(1)
        }
        val second = source("r/a", byteArrayOf(2)) {
            calls += "second"
            byteArrayOf(2)
        }
        val result = async { readFolderSelection(listOf(first, second)) }
        runCurrent()
        assertEquals(listOf("first"), calls)
        release.complete(Unit)
        assertEquals(listOf("r/z", "r/a"), result.await().map { it.relativePath })
        assertEquals(listOf("first", "second"), calls)
    }

    @Test fun folder121ReaderRejectsTruncatedBody() = runTest {
        val error = assertFailsWith<FolderSelectionException> {
            readFolderSelection(listOf(source("r/a", byteArrayOf(1)) { byteArrayOf() }))
        }
        assertEquals(FolderSelectionProblem.READ_FAILED, error.problem)
    }

    @Test fun folder121ReaderRejectsExcessBodyBytes() = runTest {
        assertFailsWith<FolderSelectionException> {
            readFolderSelection(listOf(source("r/a", byteArrayOf()) { byteArrayOf(1) }))
        }
    }

    @Test fun folder121ReaderFailureDoesNotReturnEarlierFiles() = runTest {
        var completed = false
        val first = source("r/a", byteArrayOf(1)) {
            completed = true
            byteArrayOf(1)
        }
        val second = source("r/b", byteArrayOf(2)) { error("owned read failure") }
        assertFailsWith<FolderSelectionException> { readFolderSelection(listOf(first, second)) }
        assertTrue(completed)
    }

    @Test fun folder121ReaderValidatesWholeSelectionBeforeFirstRead() = runTest {
        var reads = 0
        val first = source("r/a", byteArrayOf(1)) {
            reads++
            byteArrayOf(1)
        }
        val invalid = source("r/../b", byteArrayOf(2)) {
            reads++
            byteArrayOf(2)
        }
        assertFailsWith<FolderSelectionException> { readFolderSelection(listOf(first, invalid)) }
        assertEquals(0, reads)
    }

    @Test fun folder121ReaderRejectsResourceBoundBeforeReading() = runTest {
        var reads = 0
        val file = object : FolderFileSource {
            override val metadata = FolderFileMetadata("a", "r/a", Long.MAX_VALUE, "test/binary")
            override suspend fun readBytes(): ByteArray {
                reads++
                return byteArrayOf()
            }
        }
        assertFailsWith<FolderSelectionException> { readFolderSelection(listOf(file)) }
        assertEquals(0, reads)
    }

    @Test fun folder121ReaderCancellationStopsPendingSourceAndLaterFiles() = runTest {
        var released = false
        var laterReads = 0
        val first = source("r/a", byteArrayOf(1)) {
            try {
                awaitCancellation()
            } finally {
                released = true
            }
        }
        val later = source("r/b", byteArrayOf(1)) {
            laterReads++
            byteArrayOf(1)
        }
        val job = async { readFolderSelection(listOf(first, later)) }
        runCurrent()
        job.cancelAndJoin()
        assertTrue(released)
        assertEquals(0, laterReads)
        assertFailsWith<CancellationException> { job.await() }
    }

    @Test fun folder121ReaderCancellationBeforeLaunchReadsNothing() = runTest {
        var reads = 0
        val job = async {
            readFolderSelection(
                listOf(
                    source("r/a", byteArrayOf(1)) {
                        reads++
                        byteArrayOf(1)
                    },
                ),
            )
        }
        job.cancelAndJoin()
        assertEquals(0, reads)
    }

    @Test fun folder121ReaderChecksCancellationAfterReadBeforeNextSource() = runTest {
        var laterReads = 0
        val first = source("r/a", byteArrayOf(1)) {
            currentCoroutineContext().cancel()
            byteArrayOf(1)
        }
        val later = source("r/b", byteArrayOf(1)) {
            laterReads++
            byteArrayOf(1)
        }
        val result = async { readFolderSelection(listOf(first, later)) }
        assertFailsWith<CancellationException> { result.await() }
        assertEquals(0, laterReads)
    }

    @Test fun folder121ReaderDoesNotHideUnexpectedFatalError() = runTest {
        val fatal = Error("owned fatal source control")
        val file = source("r/a", byteArrayOf(1)) { throw fatal }
        assertEquals(fatal, assertFailsWith<Error> { readFolderSelection(listOf(file)) })
    }

    @Test fun folder121ReaderSnapshotsMetadataBeforeAnySuspension() = runTest {
        var currentMetadata = FolderFileMetadata("original", "root/original", 1, "test/original")
        val file = object : FolderFileSource {
            override val metadata: FolderFileMetadata get() = currentMetadata
            override suspend fun readBytes(): ByteArray {
                currentMetadata = FolderFileMetadata("changed", "different/changed", 100, "test/changed")
                return byteArrayOf(7)
            }
        }
        val selected = readFolderSelection(listOf(file)).single()
        assertEquals("root/original", selected.relativePath)
        assertEquals("test/original", selected.mimeType)
        assertEquals(1L, selected.size)
    }

    @Test fun folder121ReaderEmptySelectionPerformsNoWork() = runTest {
        assertEquals(emptyList(), readFolderSelection(emptyList()))
    }

    private fun source(path: String, bytes: ByteArray, read: suspend () -> ByteArray = { bytes }): FolderFileSource =
        object : FolderFileSource {
            override val metadata =
                FolderFileMetadata(path.substringAfterLast('/'), path, bytes.size.toLong(), "test/binary")
            override suspend fun readBytes(): ByteArray = read()
        }
}
