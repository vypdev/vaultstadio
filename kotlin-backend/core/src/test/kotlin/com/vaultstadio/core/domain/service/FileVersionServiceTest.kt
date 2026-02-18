/**
 * VaultStadio File Version Service Tests
 */

package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.core.domain.model.FileVersionHistory
import com.vaultstadio.core.domain.model.ItemType
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.core.domain.model.VersionRetentionPolicy
import com.vaultstadio.core.domain.repository.FileVersionRepository
import com.vaultstadio.core.domain.repository.StorageItemRepository
import com.vaultstadio.core.exception.DatabaseException
import com.vaultstadio.core.exception.InvalidOperationException
import com.vaultstadio.core.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import java.io.ByteArrayInputStream
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
    fun `getVersion should return version when found`() = runTest {
        val now = Clock.System.now()
        val version = FileVersion(
            id = "v1",
            itemId = "item-1",
            versionNumber = 1,
            size = 1024,
            checksum = "c1",
            storageKey = "key1",
            createdBy = "user-1",
            createdAt = now,
            isLatest = false,
        )
        coEvery { versionRepository.findByItemAndVersion("item-1", 1) } returns version.right()

        val result = service.getVersion("item-1", 1)

        assertTrue(result.isRight())
        result.onRight { v ->
            assertEquals(1, v.versionNumber)
            assertEquals(1024, v.size)
        }
    }

    @Test
    fun `getVersion should fail when version not found`() = runTest {
        coEvery { versionRepository.findByItemAndVersion("item-1", 99) } returns null.right()

        val result = service.getVersion("item-1", 99)

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is ItemNotFoundException)
    }

    @Test
    fun `getLatestVersion should fail when no versions exist`() = runTest {
        coEvery { versionRepository.findLatest("item-1") } returns null.right()

        val result = service.getLatestVersion("item-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is ItemNotFoundException)
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
    fun `getLatestVersion should return version when found`() = runTest {
        val now = Clock.System.now()
        val latest = FileVersion(
            id = "v2",
            itemId = "item-1",
            versionNumber = 2,
            size = 2048,
            checksum = "c2",
            storageKey = "key2",
            createdBy = "user-1",
            createdAt = now,
            isLatest = true,
        )
        coEvery { versionRepository.findLatest("item-1") } returns latest.right()

        val result = service.getLatestVersion("item-1")

        assertTrue(result.isRight())
        result.onRight { v ->
            assertEquals(2, v.versionNumber)
            assertEquals(2048, v.size)
            assertTrue(v.isLatest)
        }
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

    @Test
    fun `compareVersions should return binary diff when file is not text`() = runTest {
        val now = Clock.System.now()
        val itemId = "item-1"
        val fileItem = StorageItem(
            id = itemId,
            name = "bin.pdf",
            path = "/bin.pdf",
            type = ItemType.FILE,
            ownerId = "user-1",
            createdAt = now,
            updatedAt = now,
            mimeType = "application/pdf",
        )
        val v1 = FileVersion(
            id = "v1",
            itemId = itemId,
            versionNumber = 1,
            size = 100,
            checksum = "c1",
            storageKey = "k1",
            createdBy = "user-1",
            createdAt = now,
            isLatest = false,
        )
        val v2 = FileVersion(
            id = "v2",
            itemId = itemId,
            versionNumber = 2,
            size = 200,
            checksum = "c2",
            storageKey = "k2",
            createdBy = "user-1",
            createdAt = now,
            isLatest = true,
        )

        coEvery { versionRepository.findByItemAndVersion(itemId, 1) } returns v1.right()
        coEvery { versionRepository.findByItemAndVersion(itemId, 2) } returns v2.right()
        coEvery { itemRepository.findById(itemId) } returns fileItem.right()

        val result = service.compareVersions(itemId, 1, 2)

        assertTrue(result.isRight())
        result.onRight { diff ->
            assertTrue(diff.isBinary)
            assertEquals(1, diff.fromVersion)
            assertEquals(2, diff.toVersion)
            assertEquals(100L, diff.sizeChange)
        }
    }

    @Test
    fun `getHistory should propagate when repository returns Left`() = runTest {
        coEvery { versionRepository.getHistory("item-1") } returns DatabaseException("db error").left()

        val result = service.getHistory("item-1")

        assertTrue(result.isLeft())
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

    @Test
    fun `compareVersions should fail when from version not found`() = runTest {
        coEvery { versionRepository.findByItemAndVersion("item-1", 1) } returns null.right()
        coEvery { versionRepository.findByItemAndVersion("item-1", 2) } returns null.right()

        val result = service.compareVersions("item-1", 1, 2)

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is ItemNotFoundException)
    }

    @Test
    fun `compareVersions should fail when to version not found`() = runTest {
        val now = Clock.System.now()
        val v1 = FileVersion(
            id = "v1",
            itemId = "item-1",
            versionNumber = 1,
            size = 100,
            checksum = "c1",
            storageKey = "k1",
            createdBy = "user-1",
            createdAt = now,
            isLatest = false,
        )
        coEvery { versionRepository.findByItemAndVersion("item-1", 1) } returns v1.right()
        coEvery { versionRepository.findByItemAndVersion("item-1", 2) } returns null.right()

        val result = service.compareVersions("item-1", 1, 2)

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is ItemNotFoundException)
    }

    @Test
    fun `compareVersions should return text diff when file is text`() = runTest {
        val now = Clock.System.now()
        val itemId = "item-1"
        val fileItem = StorageItem(
            id = itemId,
            name = "readme.txt",
            path = "/readme.txt",
            type = ItemType.FILE,
            ownerId = "user-1",
            createdAt = now,
            updatedAt = now,
            mimeType = "text/plain",
        )
        val v1 = FileVersion(
            id = "v1",
            itemId = itemId,
            versionNumber = 1,
            size = 10,
            checksum = "c1",
            storageKey = "k1",
            createdBy = "user-1",
            createdAt = now,
            isLatest = false,
        )
        val v2 = FileVersion(
            id = "v2",
            itemId = itemId,
            versionNumber = 2,
            size = 15,
            checksum = "c2",
            storageKey = "k2",
            createdBy = "user-1",
            createdAt = now,
            isLatest = true,
        )
        coEvery { versionRepository.findByItemAndVersion(itemId, 1) } returns v1.right()
        coEvery { versionRepository.findByItemAndVersion(itemId, 2) } returns v2.right()
        coEvery { itemRepository.findById(itemId) } returns fileItem.right()
        coEvery { storageBackend.retrieve("k1") } returns ByteArrayInputStream("line1\nline2".toByteArray()).right()
        coEvery { storageBackend.retrieve("k2") } returns ByteArrayInputStream("line1\nchanged\nline2".toByteArray()).right()

        val result = service.compareVersions(itemId, 1, 2)

        assertTrue(result.isRight())
        result.onRight { diff ->
            assertEquals(1, diff.fromVersion)
            assertEquals(2, diff.toVersion)
            assertEquals(5L, diff.sizeChange)
            assertTrue(!diff.isBinary)
            assertTrue(diff.patches.isNotEmpty())
        }
    }

    @Test
    fun `applyRetentionPolicy should return deleted version ids and delete from storage`() = runTest {
        val itemId = "item-1"
        val now = Clock.System.now()
        val oldVersion = FileVersion(
            id = "v-old",
            itemId = itemId,
            versionNumber = 1,
            size = 1024,
            checksum = "c1",
            storageKey = "storage/old/key",
            createdBy = "user-1",
            createdAt = now,
            isLatest = false,
        )
        coEvery { versionRepository.applyRetentionPolicy(itemId, VersionRetentionPolicy.DEFAULT) } returns
            listOf("v-old").right()
        coEvery { versionRepository.findById("v-old") } returns oldVersion.right()
        coEvery { storageBackend.delete(any()) } returns Unit.right()

        val result = service.applyRetentionPolicy(itemId, VersionRetentionPolicy.DEFAULT)

        assertTrue(result.isRight())
        result.onRight { deletedIds ->
            assertEquals(1, deletedIds.size)
            assertEquals("v-old", deletedIds[0])
        }
    }

    @Test
    fun `cleanupVersionsForItem should delete all version files and remove from repository`() = runTest {
        val itemId = "item-1"
        val now = Clock.System.now()
        val versions = listOf(
            FileVersion(
                id = "v1",
                itemId = itemId,
                versionNumber = 1,
                size = 100,
                checksum = "c1",
                storageKey = "key1",
                createdBy = "user-1",
                createdAt = now,
                isLatest = false,
            ),
        )
        coEvery { versionRepository.listVersions(itemId, 100, 0) } returns versions.right()
        coEvery { storageBackend.delete(any()) } returns Unit.right()
        coEvery { versionRepository.deleteAllForItem(itemId) } returns 1.right()

        val result = service.cleanupVersionsForItem(itemId)

        assertTrue(result.isRight())
        result.onRight { count -> assertEquals(1, count) }
    }
}
