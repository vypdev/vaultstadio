package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.upload.FolderSelectionException
import com.vaultstadio.app.domain.upload.FolderSelectionProblem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Native121ReadTest {
    @Test fun folder121NativeRejectsRootEntryAndSwappedLeafLinks() = runBlocking {
        native121Tree { fixture ->
            val outside = Files.createDirectory(fixture.resolve("outside"))
            Files.write(outside.resolve("private.dat"), byteArrayOf(11, 12))
            val rootLink = Files.createSymbolicLink(fixture.resolve("root-link"), outside)
            native121RefusedBeforeRead(rootLink, FolderSelectionProblem.INVALID_PATH)
            val root = Files.createDirectory(fixture.resolve("selected"))
            val link = Files.createSymbolicLink(root.resolve("entry-link"), outside.resolve("private.dat"))
            native121RefusedBeforeRead(root, FolderSelectionProblem.INVALID_PATH)
            Files.delete(link)
            val selected = Files.write(root.resolve("file.dat"), byteArrayOf(1, 2))
            var reads = 0
            val failure = assertFailsWith<FolderSelectionException> {
                readDesktopFolderFiles(root.toFile()) { path, size ->
                    reads++
                    Files.delete(selected)
                    Files.createSymbolicLink(selected, outside.resolve("private.dat"))
                    readDesktopFolderBytes(path, size)
                }
            }
            assertEquals(FolderSelectionProblem.READ_FAILED, failure.problem)
            assertEquals(1, reads)
        }
    }

    @Test fun folder121NativeReadFailureRejectsAllEarlierFiles() = runBlocking {
        native121Tree { root ->
            repeat(3) { Files.write(root.resolve("$it.dat"), byteArrayOf(it.toByte())) }
            var reads = 0
            val failure = assertFailsWith<FolderSelectionException> {
                readDesktopFolderFiles(root.toFile()) { path, size ->
                    if (++reads == 2) throw IOException("owned-private-provider-detail")
                    readDesktopFolderBytes(path, size)
                }
            }
            assertEquals(FolderSelectionProblem.READ_FAILED, failure.problem)
            assertEquals(2, reads)
            assertFalse(failure.message.orEmpty().contains("owned-private-provider-detail"))
        }
    }

    @Test fun folder121NativeRejectsGrowingAndTruncatedBodiesAgainstSnapshot() = runBlocking {
        for (actualSize in listOf(11, 2)) {
            native121Tree { root ->
                Files.write(root.resolve("changing.dat"), byteArrayOf(1, 2, 3, 4))
                var returnedBytes = 0
                val failure = assertFailsWith<FolderSelectionException> {
                    readDesktopFolderFiles(root.toFile()) { path, size ->
                        assertEquals(4L, size)
                        Files.write(path, ByteArray(actualSize) { it.toByte() })
                        readDesktopFolderBytes(path, size).also { returnedBytes = it.size }
                    }
                }
                assertEquals(FolderSelectionProblem.READ_FAILED, failure.problem)
                assertTrue(returnedBytes <= 5, "read exceeds snapshotted size plus1")
            }
        }
    }

    @Test fun folder121NativeCancellationStopsBeforeLaterSources() = runBlocking {
        native121Tree { root ->
            repeat(2) { Files.write(root.resolve("$it.dat"), byteArrayOf(42)) }
            var reads = 0
            assertFailsWith<CancellationException> {
                readDesktopFolderFiles(root.toFile()) { _, _ ->
                    reads++
                    throw CancellationException("owned read canceled")
                }
            }
            assertEquals(1, reads)
        }
    }
}
