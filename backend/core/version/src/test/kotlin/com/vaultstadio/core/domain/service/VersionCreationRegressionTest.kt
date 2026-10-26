package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VersionCreationRegressionTest : VersionRegressionFixture() {
    @Test
    fun `createVersion should create new version for existing file`() = runTest {
        val itemId = "item-1"
        val userId = "user-1"
        val now = Clock.System.now()

        val item = StorageItem(
            id = itemId,
            name = "test.txt",
            path = "/test.txt",
            type = ItemType.FILE,
            ownerId = userId,
            createdAt = now,
            updatedAt = now,
        )

        val input = CreateVersionInput(
            itemId = itemId,
            size = 1024,
            checksum = "abc123",
            storageKey = "storage/v1/file",
            comment = "New version",
        )

        coEvery { itemRepository.findById(itemId) } returns item.right()
        coEvery { versionRepository.getNextVersionNumber(itemId) } returns 1.right()
        coEvery { versionRepository.findLatest(itemId) } returns null.right()
        coEvery { versionRepository.create(any()) } answers {
            firstArg<FileVersion>().right()
        }

        val result = service.createVersion(input, userId)

        assertTrue(result.isRight())
        result.onRight { version ->
            assertEquals(itemId, version.itemId)
            assertEquals(1, version.versionNumber)
            assertEquals(1024, version.size)
            assertEquals("abc123", version.checksum)
            assertTrue(version.isLatest)
        }
    }

    @Test
    fun `createVersion should fail for non-existent item`() = runTest {
        val input = CreateVersionInput(
            itemId = "nonexistent",
            size = 1024,
            checksum = "abc123",
            storageKey = "storage/v1/file",
        )

        coEvery { itemRepository.findById("nonexistent") } returns null.right()

        val result = service.createVersion(input, "user-1")

        assertTrue(result.isLeft())
    }

    @Test
    fun `createVersion should fail for folder`() = runTest {
        val now = Clock.System.now()
        val folder = StorageItem(
            id = "folder-1",
            name = "folder",
            path = "/folder",
            type = ItemType.FOLDER,
            ownerId = "user-1",
            createdAt = now,
            updatedAt = now,
        )

        val input = CreateVersionInput(
            itemId = "folder-1",
            size = 0,
            checksum = "",
            storageKey = "",
        )

        coEvery { itemRepository.findById("folder-1") } returns folder.right()

        val result = service.createVersion(input, "user-1")

        assertTrue(result.isLeft())
    }

    @Test
    fun `createVersion should propagate when itemRepository findById returns Left`() = runTest {
        val input = CreateVersionInput("item-1", 1024, "c1", "key1")
        val repoError = ItemNotFoundException("item-1")
        coEvery { itemRepository.findById("item-1") } returns repoError.left()

        val result = service.createVersion(input, "user-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is ItemNotFoundException)
    }

    @Test
    fun `createVersion should mark current latest as not latest when it exists`() = runTest {
        val itemId = "item-1"
        val userId = "user-1"
        val now = Clock.System.now()
        val item = StorageItem(
            id = itemId,
            name = "test.txt",
            path = "/test.txt",
            type = ItemType.FILE,
            ownerId = userId,
            createdAt = now,
            updatedAt = now,
        )
        val currentLatest = FileVersion(
            id = "v1",
            itemId = itemId,
            versionNumber = 1,
            size = 512,
            checksum = "old",
            storageKey = "key1",
            createdBy = userId,
            createdAt = now,
            isLatest = true,
        )
        val input = CreateVersionInput(
            itemId = itemId,
            size = 1024,
            checksum = "abc123",
            storageKey = "storage/v2/file",
            comment = "Update",
        )
        coEvery { itemRepository.findById(itemId) } returns item.right()
        coEvery { versionRepository.getNextVersionNumber(itemId) } returns 2.right()
        coEvery { versionRepository.findLatest(itemId) } returns currentLatest.right()
        coEvery { versionRepository.update(any()) } answers { firstArg<FileVersion>().right() }
        coEvery { versionRepository.create(any()) } answers { firstArg<FileVersion>().right() }

        val result = service.createVersion(input, userId)

        assertTrue(result.isRight())
        result.onRight { version ->
            assertEquals(2, version.versionNumber)
            assertTrue(version.isLatest)
        }
    }
}
