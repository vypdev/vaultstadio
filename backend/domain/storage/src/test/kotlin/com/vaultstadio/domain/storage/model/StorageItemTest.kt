/**
 * Unit tests for [StorageItem], [FolderContents], [StorageQuota] and related domain models.
 */

package com.vaultstadio.domain.storage.model

import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StorageItemTest {

    private val now = Instant.fromEpochMilliseconds(1_600_000_000_000)

    @Test
    fun fileExtensionReturnsSuffixAfterLastDot() {
        val item = StorageItem(
            id = "1",
            name = "photo.jpg",
            path = "/photo.jpg",
            type = ItemType.FILE,
            ownerId = "u1",
            createdAt = now,
            updatedAt = now,
        )
        assertEquals("jpg", item.extension)
    }

    @Test
    fun fileWithNoExtensionReturnsNull() {
        val item = StorageItem(
            id = "1",
            name = "README",
            path = "/README",
            type = ItemType.FILE,
            ownerId = "u1",
            createdAt = now,
            updatedAt = now,
        )
        assertNull(item.extension)
    }

    @Test
    fun folderExtensionReturnsNull() {
        val item = StorageItem(
            id = "1",
            name = "docs",
            path = "/docs",
            type = ItemType.FOLDER,
            ownerId = "u1",
            createdAt = now,
            updatedAt = now,
        )
        assertNull(item.extension)
    }

    @Test
    fun isRootIsTrueWhenParentIdIsNull() {
        val item = StorageItem(
            id = "1",
            name = "root",
            path = "/",
            type = ItemType.FOLDER,
            ownerId = "u1",
            createdAt = now,
            updatedAt = now,
            parentId = null,
        )
        assertTrue(item.isRoot)
    }

    @Test
    fun isRootIsFalseWhenParentIdIsSet() {
        val item = StorageItem(
            id = "1",
            name = "child",
            path = "/parent/child",
            type = ItemType.FOLDER,
            ownerId = "u1",
            createdAt = now,
            updatedAt = now,
            parentId = "parent-id",
        )
        assertFalse(item.isRoot)
    }

    @Test
    fun parentPathReturnsPathBeforeLastSlash() {
        val item = StorageItem(
            id = "1",
            name = "file.txt",
            path = "/a/b/file.txt",
            type = ItemType.FILE,
            ownerId = "u1",
            createdAt = now,
            updatedAt = now,
        )
        assertEquals("/a/b", item.parentPath)
    }

    @Test
    fun parentPathAtRootReturnsEmpty() {
        val item = StorageItem(
            id = "1",
            name = "only",
            path = "/only",
            type = ItemType.FILE,
            ownerId = "u1",
            createdAt = now,
            updatedAt = now,
        )
        assertEquals("", item.parentPath)
    }

    @Test
    fun storageQuotaUsagePercentage() {
        val quota = StorageQuota(
            userId = "u1",
            usedBytes = 500,
            quotaBytes = 1000,
            fileCount = 10,
            folderCount = 2,
        )
        assertEquals(50.0, quota.usagePercentage)
        assertFalse(quota.isQuotaExceeded)
        assertEquals(500, quota.remainingBytes)
    }

    @Test
    fun storageQuotaExceeded() {
        val quota = StorageQuota(
            userId = "u1",
            usedBytes = 1000,
            quotaBytes = 500,
            fileCount = 5,
            folderCount = 1,
        )
        assertTrue(quota.isQuotaExceeded)
        assertEquals(0, quota.remainingBytes)
    }

    @Test
    fun storageQuotaNoLimit() {
        val quota = StorageQuota(
            userId = "u1",
            usedBytes = 100,
            quotaBytes = null,
            fileCount = 1,
            folderCount = 0,
        )
        assertEquals(0.0, quota.usagePercentage)
        assertFalse(quota.isQuotaExceeded)
        assertNull(quota.remainingBytes)
    }

    @Test
    fun folderContentsHoldsFolderAndChildren() {
        val folder = StorageItem(
            id = "f1",
            name = "docs",
            path = "/docs",
            type = ItemType.FOLDER,
            ownerId = "u1",
            createdAt = now,
            updatedAt = now,
        )
        val contents = FolderContents(
            folder = folder,
            children = emptyList(),
            totalSize = 0,
            itemCount = 0,
        )
        assertEquals(folder, contents.folder)
        assertEquals(0, contents.children.size)
    }
}
