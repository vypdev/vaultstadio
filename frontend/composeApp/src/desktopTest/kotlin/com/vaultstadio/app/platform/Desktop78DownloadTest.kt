package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.storage.transfer.DownloadLimits
import com.vaultstadio.app.domain.storage.transfer.DownloadPayload
import com.vaultstadio.app.domain.storage.transfer.SaveOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Desktop78DownloadTest {
    @Test fun transfer78DesktopSavesBinaryBytesAndSuggestsUnicodeName() = runTest {
        withDirectory { directory ->
            val target = directory.resolve("elegido ñ.bin")
            var suggested = ""
            val saver = DesktopDownloadSaver {
                suggested = it
                target
            }
            try {
                val bytes = byteArrayOf(0, -1, 127, 4)
                assertEquals(SaveOutcome.SAVED, saver.save(payload(bytes, "original ñ.bin")))
                assertEquals("original ñ.bin", suggested)
                assertContentEquals(bytes, Files.readAllBytes(target))
                assertEquals(listOf(target), children(directory))
            } finally {
                saver.close()
            }
        }
    }

    @Test fun transfer78DesktopSavesAnActualEmptyFile() = runTest {
        withDirectory { directory ->
            val target = directory.resolve("empty")
            val saver = DesktopDownloadSaver { target }
            try {
                assertEquals(SaveOutcome.SAVED, saver.save(payload(byteArrayOf())))
                assertTrue(Files.exists(target))
                assertEquals(0L, Files.size(target))
                assertEquals(listOf(target), children(directory))
            } finally {
                saver.close()
            }
        }
    }

    @Test fun transfer78DesktopCancelDoesNotCreateOrOverwriteAnyFile() = runTest {
        withDirectory { directory ->
            val existing = directory.resolve("existing")
            Files.write(existing, byteArrayOf(8))
            val saver = DesktopDownloadSaver { null }
            try {
                assertEquals(SaveOutcome.CANCELED, saver.save(payload()))
                assertContentEquals(byteArrayOf(8), Files.readAllBytes(existing))
                assertEquals(listOf(existing), children(directory))
            } finally {
                saver.close()
            }
        }
    }

    @Test fun transfer78DesktopRejectsPathNamesBeforeShowingChooser() = runTest {
        var choices = 0
        val saver = DesktopDownloadSaver {
            choices++
            null
        }
        try {
            assertFailsWith<IllegalArgumentException> { saver.save(payload(name = "../escape")) }
            assertEquals(0, choices)
        } finally {
            saver.close()
        }
    }

    @Test fun transfer78DesktopRejectsOversizeBeforeShowingChooser() = runTest {
        var choices = 0
        val saver = DesktopDownloadSaver {
            choices++
            null
        }
        try {
            assertFailsWith<IllegalArgumentException> {
                saver.save(payload(ByteArray(DownloadLimits.MAX_BYTES + 1)))
            }
            assertEquals(0, choices)
        } finally {
            saver.close()
        }
    }

    @Test fun transfer78DesktopClosedSaverRejectsWithoutChooser() = runTest {
        var choices = 0
        val saver = DesktopDownloadSaver {
            choices++
            null
        }
        saver.close()
        saver.close()
        assertFailsWith<IllegalStateException> { saver.save(payload()) }
        assertEquals(0, choices)
    }

    @Test fun transfer78AtomicWriterReplacesOnlyWithCompletePayloadAndRemovesStaging() {
        withDirectory { directory ->
            val target = directory.resolve("existing")
            Files.write(target, byteArrayOf(9))
            val bytes = ByteArray(20001) { (it % 251).toByte() }
            writeDownloadAtomically(target, bytes)
            assertContentEquals(bytes, Files.readAllBytes(target))
            assertEquals(listOf(target), children(directory))
        }
    }

    @Test fun transfer78AtomicWriterCancellationMidWritePreservesExistingFileAndRemovesStaging() {
        withDirectory { directory ->
            val target = directory.resolve("existing")
            Files.write(target, byteArrayOf(9))
            var checks = 0
            assertFailsWith<CancellationException> {
                writeDownloadAtomically(target, ByteArray(20001)) {
                    if (++checks == 2) throw CancellationException("cancel test")
                }
            }
            assertContentEquals(byteArrayOf(9), Files.readAllBytes(target))
            assertEquals(listOf(target), children(directory))
        }
    }

    @Test fun transfer78AtomicWriterCancellationBeforePublishCreatesNoDestination() {
        withDirectory { directory ->
            val target = directory.resolve("new")
            var checks = 0
            assertFailsWith<CancellationException> {
                writeDownloadAtomically(target, byteArrayOf(1)) {
                    if (++checks == 2) throw CancellationException("cancel before publish")
                }
            }
            assertFalse(Files.exists(target))
            assertTrue(children(directory).isEmpty())
        }
    }

    @Test fun transfer78AtomicWriterPublishFailurePreservesDirectoryAndRemovesStaging() {
        withDirectory { directory ->
            val target = Files.createDirectory(directory.resolve("existing-directory"))
            Files.write(target.resolve("keep"), byteArrayOf(5))
            assertFailsWith<IOException> { writeDownloadAtomically(target, byteArrayOf(1)) }
            assertContentEquals(byteArrayOf(5), Files.readAllBytes(target.resolve("keep")))
            assertEquals(listOf(target), children(directory))
        }
    }

    @Test fun transfer78AtomicWriterMissingParentFailsWithoutCreatingDestination() {
        withDirectory { directory ->
            val target = directory.resolve("missing/file")
            assertFailsWith<IOException> { writeDownloadAtomically(target, byteArrayOf(1)) }
            assertTrue(children(directory).isEmpty())
        }
    }

    private fun payload(bytes: ByteArray = byteArrayOf(1), name: String = "file.bin") =
        DownloadPayload(name, bytes, "application/octet-stream")

    private fun children(directory: Path): List<Path> = Files.list(directory).use { it.toList() }

    private inline fun <T> withDirectory(block: (Path) -> T): T {
        val directory = Files.createTempDirectory("vaultstadio-transfer78-")
        try {
            return block(directory)
        } finally {
            Files.walk(directory).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::delete) }
        }
    }
}
