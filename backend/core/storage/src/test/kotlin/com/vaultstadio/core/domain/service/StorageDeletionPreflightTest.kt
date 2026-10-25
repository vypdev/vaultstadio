package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.event.FileEvent
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.InvalidOperationException
import com.vaultstadio.domain.common.exception.StorageBackendException
import com.vaultstadio.domain.storage.model.ItemType
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertIs
import kotlin.test.assertSame

class StorageDeletionPreflightTest {
    @Test
    fun `atomic leaf rejection never invokes byte deletion`() = runTest {
        val f = StorageAuthorizationFixture()
        val parent = item(type = ItemType.FILE).copy(storageKey = "retained-key")
        coEvery { f.repository.findById("parent") } returns parent.right()
        coEvery { f.repository.deleteLeaf("parent", ACTOR, any()) } returns
            InvalidOperationException("delete-nonempty-folder").left()
        assertIs<InvalidOperationException>(assertIs<Either.Left<*>>(f.service.deleteItem("parent", ACTOR)).value)
        f.assertNoContentEffects()
    }

    @Test
    fun `atomic repository failure is propagated before deleting bytes`() = runTest {
        val f = StorageAuthorizationFixture()
        val parent = item(type = ItemType.FILE).copy(storageKey = "retained-key")
        val failure = DatabaseException("Unavailable")
        coEvery { f.repository.findById("parent") } returns parent.right()
        coEvery { f.repository.deleteLeaf("parent", ACTOR, any()) } returns failure.left()
        assertSame(failure, assertIs<Either.Left<*>>(f.service.deleteItem("parent", ACTOR)).value)
        f.assertNoContentEffects()
    }

    @Test
    fun `backend delete failure preserves its type and prevents success events`() = runTest {
        val f = StorageAuthorizationFixture()
        val parent = item(type = ItemType.FILE).copy(storageKey = "retained-key")
        val failure = StorageBackendException("test", "Unavailable")
        coEvery { f.repository.findById("parent") } returns parent.right()
        f.allowLeafDeletion(parent)
        coEvery { f.backend.delete("retained-key") } returns failure.left()
        assertSame(failure, assertIs<Either.Left<*>>(f.service.deleteItem("parent", ACTOR)).value)
        coVerify(exactly = 0) { f.events.publish(any()) }
    }

    @Test
    fun `file without a storage key deletes metadata without deleting unrelated bytes`() = runTest {
        val f = StorageAuthorizationFixture()
        val file = item(type = ItemType.FILE)
        coEvery { f.repository.findById("parent") } returns file.right()
        f.allowLeafDeletion(file)
        assertIs<Either.Right<*>>(f.service.deleteItem("parent", ACTOR))
        coVerify(exactly = 0) { f.backend.delete(any()) }
        coVerify(exactly = 1) {
            f.repository.deleteLeaf("parent", ACTOR, any())
            f.events.publish(any())
        }
    }

    @Test
    fun `content deletion and event use the locked snapshot rather than a stale key`() = runTest {
        val f = StorageAuthorizationFixture()
        val stale = item(type = ItemType.FILE).copy(storageKey = "old-key")
        val current = stale.copy(storageKey = "current-key", version = 2)
        coEvery { f.repository.findById("parent") } returns stale.right()
        f.allowLeafDeletion(current)
        coEvery { f.backend.delete("current-key") } returns Unit.right()
        assertIs<Either.Right<*>>(f.service.deleteItem("parent", ACTOR))
        coVerify(exactly = 0) { f.backend.delete("old-key") }
        coVerify(exactly = 1) { f.events.publish(match<FileEvent.Deleted> { it.item == current }) }
    }
}
