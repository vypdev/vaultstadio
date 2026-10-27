package com.vaultstadio.infrastructure.storage

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.service.StorageBackend
import com.vaultstadio.domain.common.exception.StorageBackendException
import com.vaultstadio.domain.share.access.PublicShareFailure
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.IOException
import java.io.InputStream
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShareContentReaderAdapterTest {
    private val now = Instant.parse("2026-10-27T00:00:00Z")
    private val item = StorageItem(
        id = "item",
        name = "file",
        path = "/file",
        type = ItemType.FILE,
        ownerId = "owner",
        storageKey = "private-key",
        createdAt = now,
        updatedAt = now,
    )
    private val backend = mockk<StorageBackend>()
    private val reader = ShareContentReaderAdapter(backend)

    @ParameterizedTest(name = "S90-content-exact-bytes {0}")
    @ValueSource(ints = [0, 1, 8191, 8192, 8193, 20000])
    fun exactBinaryLengthStreamsAndCloses(size: Int) = runTest {
        val bytes = ByteArray(size) { (it % 256).toByte() }
        val source = ObservedStream(bytes)
        coEvery { backend.retrieve("private-key") } returns source.right()
        val prepared = reader.prepare(item.copy(size = size.toLong())).getOrNull()!!
        assertTrue(source.position <= 8192)
        assertContentEquals(bytes, prepared.stream.readBytes())
        assertEquals(-1, prepared.stream.read())
        prepared.close()
        prepared.close()
        assertTrue(source.closed)
    }

    @ParameterizedTest(name = "S90-content-early-length-failure {0}")
    @ValueSource(strings = ["empty-truncated", "empty-extra", "small-truncated", "small-extra"])
    fun earlyMismatchIsDeniedAndClosedBeforeAdmission(case: String) = runTest {
        val expected = if (case.startsWith("empty")) 0 else 5
        val actual = when (case) {
            "empty-truncated" -> 0
            "empty-extra" -> 1
            "small-truncated" -> 4
            else -> 6
        }
        val declared = if (case == "empty-truncated") 1 else expected
        val source = ObservedStream(ByteArray(actual))
        coEvery { backend.retrieve(any()) } returns source.right()
        assertEquals(Either.Left(PublicShareFailure.UNAVAILABLE), reader.prepare(item.copy(size = declared.toLong())))
        assertTrue(source.closed)
    }

    @ParameterizedTest(name = "S90-content-late-length-failure {0}")
    @ValueSource(ints = [8999, 9001])
    fun lateMismatchNeverForwardsExcessBytes(actual: Int) = runTest {
        val source = ObservedStream(ByteArray(actual))
        coEvery { backend.retrieve(any()) } returns source.right()
        val prepared = reader.prepare(item.copy(size = 9000)).getOrNull()!!
        try {
            assertFailsWith<IOException> { prepared.stream.readBytes() }
        } finally {
            prepared.close()
        }
        assertTrue(source.closed)
    }

    @ParameterizedTest(name = "S90-content-provider-I/O-failure at {0}")
    @ValueSource(ints = [0, 4, 8193])
    fun providerFailureHasBoundedPreparationAndOwnedCleanup(failAt: Int) = runTest {
        val source = ObservedStream(ByteArray(20000), failAt)
        coEvery { backend.retrieve(any()) } returns source.right()
        val result = reader.prepare(item.copy(size = 20000))
        if (failAt < 8192) {
            assertEquals(Either.Left(PublicShareFailure.UNAVAILABLE), result)
        } else {
            val prepared = result.getOrNull()!!
            assertFalse(source.closed)
            try {
                assertFailsWith<IOException> { prepared.stream.readBytes() }
            } finally {
                prepared.close()
            }
        }
        assertTrue(source.closed)
    }

    @Test
    fun `S90 missing content key cannot invoke provider`() = runTest {
        assertEquals(Either.Left(PublicShareFailure.NOT_FOUND), reader.prepare(item.copy(storageKey = null)))
        coVerify(exactly = 0) { backend.retrieve(any()) }
    }

    @Test
    fun `S90 provider error is safe and has no raw diagnostic`() = runTest {
        coEvery { backend.retrieve(any()) } returns StorageBackendException("secret-provider", "private-key").left()
        assertEquals(Either.Left(PublicShareFailure.UNAVAILABLE), reader.prepare(item))
    }

    @Test
    fun `S90 single-byte read and zero-length read obey InputStream contract`() = runTest {
        coEvery { backend.retrieve(any()) } returns ObservedStream(byteArrayOf(-1, 0, 1)).right()
        val prepared = reader.prepare(item.copy(size = 3)).getOrNull()!!
        prepared.use {
            assertEquals(0, it.stream.read(ByteArray(2), 0, 0))
            assertEquals(255, it.stream.read())
            assertEquals(0, it.stream.read())
            assertEquals(1, it.stream.read())
            assertEquals(-1, it.stream.read())
        }
    }

    @Test
    fun `S90 Long MAX size does not overflow prefix bound`() = runTest {
        coEvery { backend.retrieve(any()) } returns ObservedStream(ByteArray(9000)).right()
        val prepared = reader.prepare(item.copy(size = Long.MAX_VALUE)).getOrNull()!!
        prepared.close()
    }

    @Test
    fun `S90 retrieval cancellation is propagated`() = runTest {
        coEvery { backend.retrieve(any()) } throws CancellationException("test")
        assertFailsWith<CancellationException> { reader.prepare(item) }
    }

    @Test
    fun `S90 cancellation after provider opens retains ownership until close`() = runTest {
        val source = ObservedStream(byteArrayOf(1))
        val job = Job()
        coEvery { backend.retrieve(any()) } coAnswers {
            job.cancel()
            source.right()
        }
        assertFailsWith<CancellationException> {
            withContext(job) { reader.prepare(item.copy(size = 1)) }
        }
        assertTrue(source.closed)
    }

    @Test
    fun `S90 cancellation during prefix preparation closes the provider`() = runTest {
        val job = Job()
        var closed = false
        val source = object : InputStream() {
            override fun read(): Int {
                job.cancel()
                return -1
            }
            override fun close() {
                closed = true
            }
        }
        coEvery { backend.retrieve(any()) } returns source.right()
        assertFailsWith<CancellationException> {
            withContext(job) { reader.prepare(item.copy(size = 1)) }
        }
        assertTrue(closed)
    }

    @Test
    fun `S90 close failure cannot replace a denied preparation outcome`() = runTest {
        var closed = false
        val source = object : InputStream() {
            override fun read(): Int = throw IOException("private read failure")
            override fun close() {
                closed = true
                throw IOException("private close failure")
            }
        }
        coEvery { backend.retrieve(any()) } returns source.right()
        assertEquals(Either.Left(PublicShareFailure.UNAVAILABLE), reader.prepare(item.copy(size = 1)))
        assertTrue(closed)
    }

    private class ObservedStream(
        private val bytes: ByteArray,
        private val failAt: Int = Int.MAX_VALUE,
    ) : InputStream() {
        var position = 0
        var closed = false
        override fun read(): Int {
            if (position >= failAt) throw IOException("private provider failure")
            return if (position == bytes.size) -1 else bytes[position++].toInt() and 255
        }
        override fun close() {
            closed = true
        }
    }
}
