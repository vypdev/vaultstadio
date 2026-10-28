package com.vaultstadio.infrastructure.storage

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.StorageBackendException
import com.vaultstadio.domain.storage.model.ArchiveLimits
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.IOException
import java.io.InputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class Archive78WriterFailureTest {
    @Test fun `short actual content fails and closes its stream`() = runBlocking {
        val stream = Archive78Input(byteArrayOf(1))
        val result = BoundedZipArchiveWriter().write(listOf(archive78Entry(size = 3))) { stream.right() }
        assertEquals(400, (result as Either.Left).value.httpStatus)
        assertTrue(stream.closed)
    }

    @Test fun `longer actual content cannot bypass the metadata size`() = runBlocking {
        val stream = Archive78Input(byteArrayOf(1, 2, 3))
        val result = BoundedZipArchiveWriter().write(listOf(archive78Entry(size = 1))) { stream.right() }
        assertEquals(400, (result as Either.Left).value.httpStatus)
        assertTrue(stream.closed)
    }

    @Test fun `actual aggregate size is capped across streams`() = runBlocking {
        val first = Archive78Input(byteArrayOf(1, 2, 3))
        val second = Archive78Input(byteArrayOf(4, 5, 6))
        val entries = listOf(archive78Entry("first"), archive78Entry("second"))
        val result = BoundedZipArchiveWriter(ArchiveLimits(inputBytes = 5)).write(entries) {
            (if (it.path == "first") first else second).right()
        }
        assertEquals(413, (result as Either.Left).value.httpStatus)
        assertTrue(first.closed && second.closed)
    }

    @Test fun `encoded output cannot exceed the hard bound even in ZIP headers`() = runBlocking {
        var opens = 0
        val result = BoundedZipArchiveWriter(ArchiveLimits(outputBytes = 10)).write(listOf(archive78Entry())) {
            opens++
            Archive78Input(byteArrayOf(1, 2, 3)).right()
        }
        assertEquals(413, (result as Either.Left).value.httpStatus)
        assertEquals(0, opens)
    }

    @Test fun `provider identity survives a second failure while closing the ZIP`() = runBlocking {
        val failure = DatabaseException("private provider diagnostic")
        val result = BoundedZipArchiveWriter(ArchiveLimits(outputBytes = 80)).write(listOf(archive78Entry("a.txt"))) {
            failure.left()
        }
        assertSame(failure, (result as Either.Left).value)
    }

    @Test fun `duplicate ZIP paths fail before opening either stream`() = runBlocking {
        val result = BoundedZipArchiveWriter().write(listOf(archive78Entry(), archive78Entry())) { error("No content") }
        assertEquals(400, (result as Either.Left).value.httpStatus)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "../outside", "folder/../outside", "folder\\escape",
            "C:", "C:/escape", "C:relative", "safe/C:/nested",
        ],
    )
    fun `unsafe extraction paths cannot reach a stream`(name: String) = runBlocking {
        val result = BoundedZipArchiveWriter().write(listOf(archive78Entry(name))) { error("No content") }
        assertEquals(400, (result as Either.Left).value.httpStatus)
    }

    @Test fun `read failure is bounded privately and closes the input`() = runBlocking {
        var closed = false
        val input = object : InputStream() {
            override fun read(): Int = throw IOException("private/path credential-shaped-data")
            override fun close() {
                closed = true
            }
        }
        val result = BoundedZipArchiveWriter().write(listOf(archive78Entry())) { input.right() }
        val error = assertIs<StorageBackendException>((result as Either.Left).value)
        assertFalse(error.message.contains("private/path"))
        assertTrue(closed)
    }

    @Test fun `nonadvancing provider input fails rather than spinning`() = runBlocking {
        var closed = false
        val input = object : InputStream() {
            override fun read() = 0
            override fun read(bytes: ByteArray, offset: Int, length: Int) = 0
            override fun close() {
                closed = true
            }
        }
        val result = BoundedZipArchiveWriter().write(listOf(archive78Entry())) { input.right() }
        assertEquals(400, (result as Either.Left).value.httpStatus)
        assertTrue(closed)
    }
}
