package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.InvalidOperationException
import com.vaultstadio.domain.common.exception.StorageBackendException
import com.vaultstadio.domain.storage.model.ItemType
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class VersionProviderFailureTest {
    @Test
    fun `delete provider failure propagates and preserves metadata`() = runTest {
        val f = VersionServiceFixture()
        val failure = StorageBackendException("fixture", "private-key-diagnostic")
        coEvery { f.backend.delete("key1") } returns failure.left()
        assertSame(failure, assertIs<Either.Left<*>>(f.service.deleteVersion("v1", "owner")).value)
        coVerify(exactly = 0) { f.versions.delete(any()) }
        assertEquals(2, f.rows.size)
    }

    @Test
    fun `cleanup provider failure stops before row deletion`() = runTest {
        val f = VersionServiceFixture()
        val failure = StorageBackendException("fixture")
        coEvery { f.backend.delete(any()) } returns failure.left()
        assertSame(failure, assertIs<Either.Left<*>>(f.service.cleanupVersionsForItem("item", "owner")).value)
        coVerify(exactly = 0) { f.versions.deleteAllForItem(any()) }
    }

    @Test
    fun `diff stops on first provider error and closes earlier streams on second error`() = runTest {
        for (key in listOf("key1", "key2")) {
            val f = VersionServiceFixture()
            val failure = StorageBackendException("fixture")
            coEvery { f.backend.retrieve(key) } returns failure.left()
            assertSame(failure, assertIs<Either.Left<*>>(f.service.compareVersions("item", 1, 2, "owner")).value)
            if (key == "key1") {
                coVerify(exactly = 0) { f.backend.retrieve("key2") }
            } else {
                assertEquals(listOf("key1"), f.closedStreams)
            }
        }
    }

    @Test
    fun `stream I O failure becomes typed provider failure with resource closure`() = runTest {
        val f = VersionServiceFixture()
        var closed = false
        val stream = object : ByteArrayInputStream(byteArrayOf(1)) {
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int = throw IOException("private path")
            override fun close() {
                closed = true
                super.close()
            }
        }
        coEvery { f.backend.retrieve("key1") } returns stream.right()
        assertIs<StorageBackendException>(
            assertIs<Either.Left<*>>(f.service.compareVersions("item", 1, 2, "owner")).value,
        )
        assertTrue(closed)
        coVerify(exactly = 0) { f.backend.retrieve("key2") }
    }

    @Test
    fun `owner file type and trashed read semantics remain explicit`() = runTest {
        val f = VersionServiceFixture()
        f.item = f.item.copy(type = ItemType.FOLDER)
        assertIs<InvalidOperationException>(
            assertIs<Either.Left<*>>(
                f.service.createVersion(CreateVersionInput("item", 4, "c", "key1"), "owner"),
            ).value,
        )
        f.item = f.item.copy(type = ItemType.FILE, isTrashed = true)
        assertIs<Either.Right<*>>(f.service.getHistory("item", "owner"))
    }

    @Test
    fun `latest lookup rejects a nonlatest row`() = runTest {
        val f = VersionServiceFixture()
        coEvery { f.versions.findLatest("item") } returns f.rows.first().right()
        assertIs<DatabaseException>(
            assertIs<Either.Left<*>>(f.service.getLatestVersion("item", "owner")).value,
        )
    }

    @Test
    fun `cleanup covers more than a default page`() = runTest {
        val f = VersionServiceFixture()
        for (number in 3..103) {
            val row = f.rows.first().copy(id = "v$number", versionNumber = number, storageKey = "key$number")
            f.rows.add(row)
            f.bytes[row.storageKey] = byteArrayOf(number.toByte())
        }
        assertEquals(103, assertIs<Either.Right<*>>(f.service.cleanupVersionsForItem("item", "owner")).value)
        assertTrue(f.rows.isEmpty())
        assertEquals(setOf("key2"), f.bytes.keys)
    }

    @Test
    fun `stream close failure stays typed and prevents opening the next version`() = runTest {
        val f = VersionServiceFixture()
        var closed = false
        val stream = object : ByteArrayInputStream("old\n".toByteArray()) {
            override fun close() {
                closed = true
                super.close()
                throw IOException("private close diagnostic")
            }
        }
        coEvery { f.backend.retrieve("key1") } returns stream.right()
        assertIs<StorageBackendException>(
            assertIs<Either.Left<*>>(f.service.compareVersions("item", 1, 2, "owner")).value,
        )
        assertTrue(closed)
        coVerify(exactly = 0) { f.backend.retrieve("key2") }
    }
}
