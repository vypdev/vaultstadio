package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.upload.FOLDER_SELECTION_MAX_BYTES
import com.vaultstadio.app.domain.upload.FOLDER_SELECTION_MAX_DEPTH
import com.vaultstadio.app.domain.upload.FOLDER_SELECTION_MAX_ENTRIES
import com.vaultstadio.app.domain.upload.FolderSelectionProblem
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Native121ResourcesTest {
    @Test fun folder121NativePreservesBasicBytesUnicodeAndEmptyFiles() = runBlocking {
        native121Tree { root ->
            val nested = Files.createDirectories(root.resolve("niño 日本語"))
            val bytes = byteArrayOf(0, 42, -1, 7)
            Files.write(nested.resolve("binary.bin"), bytes)
            Files.write(root.resolve("empty.dat"), byteArrayOf())
            val result = readDesktopFolderFiles(root.toFile())
            assertEquals(setOf("niño 日本語/binary.bin", "empty.dat"), result.map { it.relativePath }.toSet())
            assertContentEquals(bytes, result.single { it.name == "binary.bin" }.data)
            assertEquals(4L, result.single { it.name == "binary.bin" }.size)
            assertTrue(result.single { it.name == "empty.dat" }.data.isEmpty())
            val empty = Files.createDirectory(root.resolve("omitted-empty"))
            assertTrue(readDesktopFolderFiles(empty.toFile()).isEmpty())
            native121RefusedBeforeRead(root.resolve("empty.dat"), FolderSelectionProblem.READ_FAILED)
        }
    }

    @Test fun folder121NativeReadsEveryByteAtExactAggregateLimit() = runBlocking {
        native121Tree { root ->
            val body = ByteArray((FOLDER_SELECTION_MAX_BYTES / 2).toInt()) { it.toByte() }
            Files.write(root.resolve("first.bin"), body)
            Files.write(root.resolve("second.bin"), body)
            val result = readDesktopFolderFiles(root.toFile())
            assertEquals(2, result.size)
            assertEquals(FOLDER_SELECTION_MAX_BYTES, result.sumOf { it.size })
            result.forEach { assertContentEquals(body, it.data) }
        }
    }

    @Test fun folder121NativeAggregatePlusOneRefusesBeforeRead() = runBlocking {
        native121Tree { root ->
            native121Sized(root.resolve("first.bin"), FOLDER_SELECTION_MAX_BYTES / 2)
            native121Sized(root.resolve("second.bin"), FOLDER_SELECTION_MAX_BYTES / 2 + 1)
            native121RefusedBeforeRead(root, FolderSelectionProblem.TOO_LARGE)
        }
    }

    @Test fun folder121NativeOversizedFileRefusesInsteadOfSkipping() = runBlocking {
        native121Tree { root ->
            native121Sized(root.resolve("large.bin"), FOLDER_SELECTION_MAX_BYTES + 1)
            native121RefusedBeforeRead(root, FolderSelectionProblem.TOO_LARGE)
        }
    }

    @Test fun folder121NativeAcceptsExactVisitedEntryLimit() = runBlocking {
        native121Tree { root ->
            repeat(FOLDER_SELECTION_MAX_ENTRIES - 1) { Files.createFile(root.resolve("$it.dat")) }
            val result = readDesktopFolderFiles(root.toFile())
            assertEquals(FOLDER_SELECTION_MAX_ENTRIES - 1, result.size)
            assertTrue(result.all { it.size == 0L && it.data.isEmpty() })
        }
    }

    @Test fun folder121NativeVisitedEntryPlusOneRefusesBeforeRead() = runBlocking {
        for (directories in listOf(false, true)) {
            native121Tree { root ->
                repeat(FOLDER_SELECTION_MAX_ENTRIES) {
                    if (directories) {
                        Files.createDirectory(
                            root.resolve("$it"),
                        )
                    } else {
                        Files.createFile(root.resolve("$it.dat"))
                    }
                }
                native121RefusedBeforeRead(root, FolderSelectionProblem.TOO_MANY_ENTRIES)
            }
        }
    }

    @Test fun folder121NativeAcceptsExactRelativeDepth() = runBlocking {
        native121Tree { root ->
            var parent = root
            repeat(FOLDER_SELECTION_MAX_DEPTH - 1) { parent = Files.createDirectory(parent.resolve("level$it")) }
            Files.write(parent.resolve("leaf.dat"), byteArrayOf(42))
            val result = readDesktopFolderFiles(root.toFile()).single()
            assertEquals(FOLDER_SELECTION_MAX_DEPTH, result.relativePath.split('/').size)
            assertContentEquals(byteArrayOf(42), result.data)
        }
    }

    @Test fun folder121NativeRelativeDepthPlusOneRefusesBeforeRead() = runBlocking {
        native121Tree { root ->
            var parent = root
            repeat(FOLDER_SELECTION_MAX_DEPTH) { parent = Files.createDirectory(parent.resolve("level$it")) }
            Files.write(parent.resolve("leaf.dat"), byteArrayOf(42))
            native121RefusedBeforeRead(root, FolderSelectionProblem.TOO_DEEP)
        }
    }
}
