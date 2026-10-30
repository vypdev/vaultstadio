package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.right
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.domain.common.exception.InvalidOperationException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VersionMutationRegressionTest : VersionRegressionFixture() {
    @Test
    fun `restoreVersion should fail when version not found`() = runTest {
        val input = RestoreVersionInput("item-1", 99, null)
        coEvery { versionRepository.findByItemAndVersion("item-1", 99) } returns null.right()

        val result = service.restoreVersion(input, "user-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is ItemNotFoundException)
    }

    @Test
    fun `restoreVersion should fail when version is latest`() = runTest {
        val now = Clock.System.now()
        val latestVersion = FileVersion(
            id = "v1",
            itemId = "item-1",
            versionNumber = 1,
            size = 1024,
            checksum = "c1",
            storageKey = "key1",
            createdBy = "user-1",
            createdAt = now,
            isLatest = true,
        )
        val input = RestoreVersionInput("item-1", 1, null)
        coEvery { versionRepository.findByItemAndVersion("item-1", 1) } returns latestVersion.right()

        val result = service.restoreVersion(input, "user-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is InvalidOperationException)
    }

    @Test
    fun `deleteVersion should fail when version not found`() = runTest {
        coEvery { versionRepository.findById("v-missing") } returns null.right()

        val result = service.deleteVersion("v-missing", "user-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is ItemNotFoundException)
    }

    @Test
    fun `deleteVersion should fail when version is latest`() = runTest {
        val now = Clock.System.now()
        val latest = FileVersion(
            id = "v1",
            itemId = "item-1",
            versionNumber = 1,
            size = 1024,
            checksum = "c1",
            storageKey = "key1",
            createdBy = "user-1",
            createdAt = now,
            isLatest = true,
        )
        coEvery { versionRepository.findById("v1") } returns latest.right()

        val result = service.deleteVersion("v1", "user-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is InvalidOperationException)
    }

    @Test
    fun `deleteVersion should succeed when version exists and is not latest`() = runTest {
        val now = Clock.System.now()
        val oldVersion = FileVersion(
            id = "v-old",
            itemId = "item-1",
            versionNumber = 1,
            size = 1024,
            checksum = "c1",
            storageKey = "storage/old/key",
            createdBy = "user-1",
            createdAt = now,
            isLatest = false,
        )
        coEvery { versionRepository.findById("v-old") } returns oldVersion.right()
        coEvery { storageBackend.delete(any()) } returns Unit.right()
        coEvery { versionRepository.delete("v-old") } returns Unit.right()

        val result = service.deleteVersion("v-old", "user-1")

        assertTrue(result.isRight())
    }

    @Test
    fun `restoreVersion should create new version from non-latest version`() = runTest {
        val now = Clock.System.now()
        val itemId = "item-1"
        val userId = "user-1"
        val item = StorageItem(
            id = itemId,
            name = "test.txt",
            path = "/test.txt",
            type = ItemType.FILE,
            ownerId = userId,
            createdAt = now,
            updatedAt = now,
        )
        val versionToRestore = FileVersion(
            id = "v1",
            itemId = itemId,
            versionNumber = 1,
            size = 1024,
            checksum = "abc",
            storageKey = "key1",
            createdBy = userId,
            createdAt = now,
            isLatest = false,
        )
        val input = RestoreVersionInput("item-1", 1, "Restored")

        coEvery { versionRepository.findByItemAndVersion("item-1", 1) } returns versionToRestore.right()
        coEvery { itemRepository.findById(itemId) } returns item.right()
        coEvery { versionRepository.getNextVersionNumber(itemId) } returns 3.right()
        coEvery { versionRepository.findLatest(itemId) } returns null.right()
        coEvery { versionRepository.create(any()) } answers { firstArg<FileVersion>().right() }

        val result = service.restoreVersion(input, userId)

        assertTrue(result.isRight())
        result.onRight { v ->
            assertEquals(itemId, v.itemId)
            assertEquals(3, v.versionNumber)
            assertEquals(1024, v.size)
            assertEquals(1, v.restoredFrom)
        }
    }
}
