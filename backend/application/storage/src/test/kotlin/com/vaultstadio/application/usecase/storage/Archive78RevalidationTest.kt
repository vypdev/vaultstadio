package com.vaultstadio.application.usecase.storage

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.service.ArchiveWriter
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.model.ArchiveEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

internal class Archive78RevalidationTest : Archive78SelectionFixture() {
    private val writer = mockk<ArchiveWriter>()
    private val operation get() = DownloadArchiveUseCaseImpl(storage, writer)

    private fun executeContentPort() {
        coEvery { writer.write(any(), any()) } coAnswers {
            val entries = firstArg<List<ArchiveEntry>>()
            val open = secondArg<suspend (ArchiveEntry) -> Either<StorageException, InputStream>>()
            open(entries.single()).map { stream -> stream.use { it.readBytes() } }
        }
    }

    @Test fun `content is opened only after complete metadata preflight`() = runBlocking {
        val item = file(size = 3)
        metadata(item)
        executeContentPort()
        coEvery { storage.downloadFile(item.id, "owner78") } returns
            (item to ByteArrayInputStream(byteArrayOf(0, 1, -1))).right()
        assertContentEquals(byteArrayOf(0, 1, -1), (operation(listOf(item.id), "owner78") as Either.Right).value)
        coVerify(exactly = 1) { storage.getItem(item.id, "owner78") }
        coVerify(exactly = 1) { storage.downloadFile(item.id, "owner78") }
    }

    @Test fun `a foreign later selected item prevents writer and earlier file reads`() = runBlocking {
        val item = file()
        metadata(item)
        val denial = AuthorizationException()
        coEvery { storage.getItem("foreign", "owner78") } returns denial.left()
        assertSame(denial, (operation(listOf(item.id, "foreign"), "owner78") as Either.Left).value)
        coVerify(exactly = 0) { writer.write(any(), any()) }
        coVerify(exactly = 0) { storage.downloadFile(any(), any()) }
    }

    @Test fun `owner denial when opening after preflight cannot become success`() = runBlocking {
        val item = file()
        metadata(item)
        executeContentPort()
        val denial = AuthorizationException()
        coEvery { storage.downloadFile(item.id, "owner78") } returns denial.left()
        assertSame(denial, (operation(listOf(item.id), "owner78") as Either.Left).value)
    }

    @ParameterizedTest
    @ValueSource(strings = ["owner", "version", "name", "parent", "key", "size", "checksum"])
    fun `changed canonical facts close the just-opened stream and reject stale archive`(changed: String) = runBlocking {
        val item = file()
        metadata(item)
        executeContentPort()
        val current = when (changed) {
            "owner" -> item.copy(ownerId = "other")
            "version" -> item.copy(version = item.version + 1)
            "name" -> item.copy(name = "renamed")
            "parent" -> item.copy(parentId = "moved")
            "key" -> item.copy(storageKey = "replacement")
            "size" -> item.copy(size = 2)
            else -> item.copy(checksum = "changed")
        }
        var closed = false
        val stream = object : ByteArrayInputStream(byteArrayOf(1)) {
            override fun close() {
                closed = true
                super.close()
            }
        }
        coEvery { storage.downloadFile(item.id, "owner78") } returns (current to stream).right()
        assertEquals(409, (operation(listOf(item.id), "owner78") as Either.Left).value.httpStatus)
        assertTrue(closed)
    }

    @Test fun `cancellation during metadata never invokes the archive writer`() = runBlocking {
        coEvery { storage.getItem("file78", "owner78") } throws CancellationException("owned canceled request")
        assertFailsWith<CancellationException> { operation(listOf("file78"), "owner78") }
        coVerify(exactly = 0) { writer.write(any(), any()) }
    }

    @Test fun `cancellation during content opening remains control flow`() = runBlocking {
        val item = file()
        metadata(item)
        executeContentPort()
        coEvery { storage.downloadFile(item.id, "owner78") } throws CancellationException("owned canceled request")
        assertFailsWith<CancellationException> { operation(listOf(item.id), "owner78") }
    }
}
