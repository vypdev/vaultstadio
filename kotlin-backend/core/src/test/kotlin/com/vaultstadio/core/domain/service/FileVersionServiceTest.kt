/**
 * VaultStadio File Version Service Tests
 */

package com.vaultstadio.core.domain.service

import arrow.core.right
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.core.domain.model.FileVersionHistory
import com.vaultstadio.core.domain.model.ItemType
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.core.domain.repository.FileVersionRepository
import com.vaultstadio.core.domain.repository.StorageItemRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileVersionServiceTest {

    private lateinit var versionRepository: FileVersionRepository
    private lateinit var itemRepository: StorageItemRepository
    private lateinit var storageBackend: StorageBackend
    private lateinit var service: FileVersionService

    @BeforeEach
    fun setup() {
        versionRepository = mockk()
        itemRepository = mockk()
        storageBackend = mockk()
        service = FileVersionService(versionRepository, itemRepository, storageBackend)
    }

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
    fun `getHistory should return version history`() = runTest {
        val itemId = "item-1"
        val now = Clock.System.now()

        val history = FileVersionHistory(
            item = StorageItem(
                id = itemId,
                name = "test.txt",
                path = "/test.txt",
                type = ItemType.FILE,
                ownerId = "user-1",
                createdAt = now,
                updatedAt = now,
            ),
            versions = listOf(
                FileVersion(
                    id = "v2",
                    itemId = itemId,
                    versionNumber = 2,
                    size = 2048,
                    checksum = "xyz789",
                    storageKey = "storage/v2/file",
                    createdBy = "user-1",
                    createdAt = now,
                    isLatest = true,
                ),
                FileVersion(
                    id = "v1",
                    itemId = itemId,
                    versionNumber = 1,
                    size = 1024,
                    checksum = "abc123",
                    storageKey = "storage/v1/file",
                    createdBy = "user-1",
                    createdAt = now,
                    isLatest = false,
                ),
            ),
            totalVersions = 2,
            totalSize = 3072,
        )

        coEvery { versionRepository.getHistory(itemId) } returns history.right()

        val result = service.getHistory(itemId)

        assertTrue(result.isRight())
        result.onRight { h ->
            assertEquals(2, h.totalVersions)
            assertEquals(3072, h.totalSize)
            assertEquals(2, h.latestVersion?.versionNumber)
        }
    }

    @Test
    fun `listVersions should return paginated versions`() = runTest {
        val itemId = "item-1"
        val now = Clock.System.now()

        val versions = listOf(
            FileVersion(
                id = "v1",
                itemId = itemId,
                versionNumber = 1,
                size = 1024,
                checksum = "abc",
                storageKey = "key1",
                createdBy = "user-1",
                createdAt = now,
                isLatest = true,
            ),
        )

        coEvery { versionRepository.listVersions(itemId, 10, 0) } returns versions.right()

        val result = service.listVersions(itemId, 10, 0)

        assertTrue(result.isRight())
        result.onRight { list ->
            assertEquals(1, list.size)
        }
    }

    @Test
    fun `getVersionCount should return count`() = runTest {
        coEvery { versionRepository.countVersions("item-1") } returns 5.right()

        val result = service.getVersionCount("item-1")

        assertTrue(result.isRight())
        result.onRight { count ->
            assertEquals(5, count)
        }
    }

    @Test
    fun `getTotalVersionSize should return total size`() = runTest {
        coEvery { versionRepository.getTotalVersionSize("item-1") } returns 10240L.right()

        val result = service.getTotalVersionSize("item-1")

        assertTrue(result.isRight())
        result.onRight { size ->
            assertEquals(10240L, size)
        }
    }
}
