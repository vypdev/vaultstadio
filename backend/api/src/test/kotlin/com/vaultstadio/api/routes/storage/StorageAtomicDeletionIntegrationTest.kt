package com.vaultstadio.api.routes.storage

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.domain.common.exception.StorageBackendException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StorageAtomicDeletionIntegrationTest {
    private val f = StorageIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @AfterAll fun close() = f.close()

    @BeforeEach fun reset() = f.reset()

    @Test
    fun `atomic leaf port revalidates ownership before the content callback`() = runBlocking<Unit> {
        val file = f.seedStoredFile()
        var called = false
        val result = f.repository.deleteLeaf(file.id, f.bob.id) {
            called = true
            Unit.right()
        }
        assertIs<AuthorizationException>(assertIs<Either.Left<*>>(result).value)
        assertFalse(called)
        assertEquals(file, assertIs<Either.Right<*>>(f.repository.findById(file.id)).value)
        assertEquals(1, f.byteFileCount())
    }

    @Test
    fun `content callback failure preserves metadata and returns its typed error`() = runBlocking<Unit> {
        val file = f.seedStoredFile()
        val failure = StorageBackendException("test", "Unavailable")
        val result = f.repository.deleteLeaf(file.id, f.alice.id) { failure.left() }
        assertSame(failure, assertIs<Either.Left<*>>(result).value)
        assertEquals(file, assertIs<Either.Right<*>>(f.repository.findById(file.id)).value)
        val retained = f.backend.retrieve(file.storageKey!!).fold({ throw it }, { it }).use { it.readBytes() }
        assertEquals("retained", retained.decodeToString())
    }

    @Test
    fun `missing locked item never invokes the content callback`() = runBlocking<Unit> {
        var called = false
        val result = f.repository.deleteLeaf(UUID.randomUUID().toString(), f.alice.id) {
            called = true
            Unit.right()
        }
        assertIs<ItemNotFoundException>(assertIs<Either.Left<*>>(result).value)
        assertFalse(called)
        assertEquals(0, f.rowCount())
        assertEquals(0, f.byteFileCount())
    }

    @Test
    fun `cancelled content callback releases the item lock without metadata deletion`() = runBlocking<Unit> {
        val file = f.seedStoredFile()
        assertFailsWith<CancellationException> {
            f.repository.deleteLeaf(file.id, f.alice.id) { throw CancellationException("test cancellation") }
        }
        assertEquals(file, assertIs<Either.Right<*>>(f.repository.findById(file.id)).value)
        assertEquals(1, f.byteFileCount())
        assertIs<Either.Right<*>>(f.repository.deleteLeaf(file.id, f.alice.id) { f.backend.delete(it.storageKey!!) })
        assertEquals(0, f.rowCount())
        assertEquals(0, f.byteFileCount())
    }
}
