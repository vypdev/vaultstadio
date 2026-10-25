package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.InvalidOperationException
import com.vaultstadio.domain.common.exception.StorageBackendException
import com.vaultstadio.domain.common.pagination.PagedResult
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertIs
import kotlin.test.assertSame

class StorageDeletionPreflightTest {
    @Test
    fun `legacy file parents cannot lose bytes before a leaf rejection`() = runTest {
        val f = StorageAuthorizationFixture()
        val parent = item(type = ItemType.FILE).copy(storageKey = "retained-key")
        val child = item("foreign-child", owner = FOREIGN, parentId = "parent")
        coEvery { f.repository.findById("parent") } returns parent.right()
        coEvery {
            f.repository.query(
                match {
                    it.parentId == "parent" &&
                        it.filterByParent &&
                        it.ownerId == null &&
                        it.limit == 1
                },
            )
        } returns
            PagedResult(listOf(child), 1, 0, 1).right()
        assertIs<InvalidOperationException>(assertIs<Either.Left<*>>(f.service.deleteItem("parent", ACTOR)).value)
        f.assertNoEffects()
    }

    @Test
    fun `child preflight repository failure is propagated before deleting bytes`() = runTest {
        val f = StorageAuthorizationFixture()
        val parent = item(type = ItemType.FILE).copy(storageKey = "retained-key")
        val failure = DatabaseException("Unavailable")
        coEvery { f.repository.findById("parent") } returns parent.right()
        coEvery { f.repository.query(any()) } returns failure.left()
        assertSame(failure, assertIs<Either.Left<*>>(f.service.deleteItem("parent", ACTOR)).value)
        f.assertNoEffects()
    }

    @Test
    fun `backend delete failure preserves its type and prevents metadata deletion`() = runTest {
        val f = StorageAuthorizationFixture()
        val parent = item(type = ItemType.FILE).copy(storageKey = "retained-key")
        val failure = StorageBackendException("test", "Unavailable")
        coEvery { f.repository.findById("parent") } returns parent.right()
        coEvery { f.repository.query(any()) } returns PagedResult<StorageItem>(emptyList(), 0, 0, 1).right()
        coEvery { f.backend.delete("retained-key") } returns failure.left()
        assertSame(failure, assertIs<Either.Left<*>>(f.service.deleteItem("parent", ACTOR)).value)
        coVerify(exactly = 0) {
            f.repository.delete(any())
            f.events.publish(any())
        }
    }

    @Test
    fun `metadata deletion failure does not emit a successful event`() = runTest {
        val f = StorageAuthorizationFixture()
        val failure = DatabaseException("Unavailable")
        coEvery { f.repository.findById("parent") } returns item().right()
        coEvery { f.repository.query(any()) } returns PagedResult<StorageItem>(emptyList(), 0, 0, 1).right()
        coEvery { f.repository.delete("parent") } returns failure.left()
        assertSame(failure, assertIs<Either.Left<*>>(f.service.deleteItem("parent", ACTOR)).value)
        coVerify(exactly = 0) {
            f.backend.delete(any())
            f.events.publish(any())
        }
    }

    @Test
    fun `file without a storage key deletes metadata without deleting unrelated bytes`() = runTest {
        val f = StorageAuthorizationFixture()
        coEvery { f.repository.findById("parent") } returns item(type = ItemType.FILE).right()
        coEvery { f.repository.query(any()) } returns PagedResult<StorageItem>(emptyList(), 0, 0, 1).right()
        coEvery { f.repository.delete("parent") } returns Unit.right()
        assertIs<Either.Right<*>>(f.service.deleteItem("parent", ACTOR))
        coVerify(exactly = 0) { f.backend.delete(any()) }
        coVerify(exactly = 1) {
            f.repository.delete("parent")
            f.events.publish(any())
        }
    }
}
