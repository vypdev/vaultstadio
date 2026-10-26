package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.right
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VersionComparisonRegressionTest : VersionRegressionFixture() {
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

        val result = service.compareVersions(itemId, 1, 2, "user-1")

        assertTrue(result.isRight())
        result.onRight { diff ->
            assertTrue(diff.isBinary)
            assertEquals(1, diff.fromVersion)
            assertEquals(2, diff.toVersion)
            assertEquals(100L, diff.sizeChange)
        }
    }

    @Test
    fun `compareVersions should fail when from version not found`() = runTest {
        coEvery { versionRepository.findByItemAndVersion("item-1", 1) } returns null.right()
        coEvery { versionRepository.findByItemAndVersion("item-1", 2) } returns null.right()

        val result = service.compareVersions("item-1", 1, 2, "user-1")

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

        val result = service.compareVersions("item-1", 1, 2, "user-1")

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
        coEvery { storageBackend.retrieve("k2") } returns
            ByteArrayInputStream("line1\nchanged\nline2".toByteArray()).right()

        val result = service.compareVersions(itemId, 1, 2, "user-1")

        assertTrue(result.isRight())
        result.onRight { diff ->
            assertEquals(1, diff.fromVersion)
            assertEquals(2, diff.toVersion)
            assertEquals(5L, diff.sizeChange)
            assertTrue(!diff.isBinary)
            assertTrue(diff.patches.isNotEmpty())
        }
    }
}
