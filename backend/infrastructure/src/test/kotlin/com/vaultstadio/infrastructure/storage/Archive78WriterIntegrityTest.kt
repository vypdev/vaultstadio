package com.vaultstadio.infrastructure.storage

import arrow.core.Either
import arrow.core.right
import com.vaultstadio.domain.storage.model.ArchiveLimits
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Archive78WriterIntegrityTest {
    @Test fun `binary content CRC and names survive actual ZIP encoding`() = runBlocking {
        val bytes = byteArrayOf(0, -1, 127)
        val input = Archive78Input(bytes)
        val result = BoundedZipArchiveWriter().write(listOf(archive78Entry("Archivo ñ.bin"))) { input.right() }
        val zip = archive78Unzip((result as Either.Right).value)
        assertEquals(setOf("Archivo ñ.bin"), zip.keys)
        assertContentEquals(bytes, zip["Archivo ñ.bin"])
        assertTrue(input.closed)
    }

    @Test fun `empty file is an entry rather than a skipped failure`() = runBlocking {
        val input = Archive78Input(byteArrayOf())
        val result = BoundedZipArchiveWriter().write(listOf(archive78Entry("empty", 0))) { input.right() }
        val zip = archive78Unzip((result as Either.Right).value)
        assertEquals(setOf("empty"), zip.keys)
        assertContentEquals(byteArrayOf(), zip["empty"])
        assertTrue(input.closed)
    }

    @Test fun `empty directory has a trailing separator and opens no content`() = runBlocking {
        val result = BoundedZipArchiveWriter().write(listOf(archive78Entry("root/empty", 0, true))) {
            error("No directory stream")
        }
        assertEquals(setOf("root/empty/"), archive78Unzip((result as Either.Right).value).keys)
    }

    @Test fun `exact aggregate byte budget succeeds`() = runBlocking {
        val input = Archive78Input(byteArrayOf(0, 1, -1))
        val result = BoundedZipArchiveWriter(ArchiveLimits(inputBytes = 3)).write(listOf(archive78Entry())) {
            input.right()
        }
        assertContentEquals(byteArrayOf(0, 1, -1), archive78Unzip((result as Either.Right).value)["bytes.bin"])
        assertTrue(input.closed)
    }
}
