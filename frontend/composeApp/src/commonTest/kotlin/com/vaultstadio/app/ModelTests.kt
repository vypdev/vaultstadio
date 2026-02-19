/**
 * VaultStadio Model Tests
 *
 * Tests for shared domain models.
 */

package com.vaultstadio.app

import com.vaultstadio.app.domain.model.ItemType
import com.vaultstadio.app.domain.model.StorageItem
import com.vaultstadio.app.domain.model.StorageQuota
import com.vaultstadio.app.domain.model.UserRole
import com.vaultstadio.app.domain.model.Visibility
import kotlin.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StorageItemTest {

    private val now = Clock.System.now()

    @Test
    fun shouldIdentifyFileType() {
        val file = StorageItem(
            id = "file-1",
            name = "document.pdf",
            path = "/documents/document.pdf",
            type = ItemType.FILE,
            parentId = "parent-1",
            size = 1024,
            mimeType = "application/pdf",
            visibility = Visibility.PRIVATE,
            isStarred = false,
            isTrashed = false,
            createdAt = now,
            updatedAt = now,
        )

        assertTrue(file.isFile)
        assertFalse(file.isFolder)
    }

    @Test
    fun shouldIdentifyFolderType() {
        val folder = StorageItem(
            id = "folder-1",
            name = "Documents",
            path = "/Documents",
            type = ItemType.FOLDER,
            parentId = null,
            size = 0,
            mimeType = null,
            visibility = Visibility.PRIVATE,
            isStarred = false,
            isTrashed = false,
            createdAt = now,
            updatedAt = now,
        )

        assertTrue(folder.isFolder)
        assertFalse(folder.isFile)
    }

    @Test
    fun shouldExtractFileExtension() {
        val file = StorageItem(
            id = "file-1",
            name = "photo.jpg",
            path = "/photo.jpg",
            type = ItemType.FILE,
            parentId = null,
            size = 1024,
            mimeType = "image/jpeg",
            visibility = Visibility.PRIVATE,
            isStarred = false,
            isTrashed = false,
            createdAt = now,
            updatedAt = now,
        )

        assertEquals("jpg", file.extension)
    }

    @Test
    fun shouldHandleFileWithMultipleDots() {
        val file = StorageItem(
            id = "file-1",
            name = "archive.tar.gz",
            path = "/archive.tar.gz",
            type = ItemType.FILE,
            parentId = null,
            size = 1024,
            mimeType = "application/gzip",
            visibility = Visibility.PRIVATE,
            isStarred = false,
            isTrashed = false,
            createdAt = now,
            updatedAt = now,
        )

        assertEquals("gz", file.extension)
    }

    @Test
    fun shouldReturnNullExtensionForFolder() {
        val folder = StorageItem(
            id = "folder-1",
            name = "Documents",
            path = "/Documents",
            type = ItemType.FOLDER,
            parentId = null,
            size = 0,
            mimeType = null,
            visibility = Visibility.PRIVATE,
            isStarred = false,
            isTrashed = false,
            createdAt = now,
            updatedAt = now,
        )

        assertNull(folder.extension)
    }
}

class StorageQuotaTest {

    @Test
    fun shouldDetectNearLimit() {
        val quota = StorageQuota(
            usedBytes = 9 * 1024 * 1024 * 1024L, // 9GB
            quotaBytes = 10 * 1024 * 1024 * 1024L, // 10GB
            usagePercentage = 90.0,
            fileCount = 100,
            folderCount = 10,
            remainingBytes = 1 * 1024 * 1024 * 1024L, // 1GB
        )

        assertTrue(quota.isNearLimit)
        assertFalse(quota.isOverLimit)
    }

    @Test
    fun shouldDetectOverLimit() {
        val quota = StorageQuota(
            usedBytes = 11 * 1024 * 1024 * 1024L, // 11GB
            quotaBytes = 10 * 1024 * 1024 * 1024L, // 10GB
            usagePercentage = 110.0,
            fileCount = 100,
            folderCount = 10,
            remainingBytes = -1 * 1024 * 1024 * 1024L, // -1GB
        )

        assertTrue(quota.isNearLimit) // 110% > 90%
        assertTrue(quota.isOverLimit)
    }

    @Test
    fun shouldNotBeNearLimitForLowUsage() {
        val quota = StorageQuota(
            usedBytes = 5 * 1024 * 1024 * 1024L, // 5GB
            quotaBytes = 10 * 1024 * 1024 * 1024L, // 10GB
            usagePercentage = 50.0,
            fileCount = 50,
            folderCount = 5,
            remainingBytes = 5 * 1024 * 1024 * 1024L, // 5GB
        )

        assertFalse(quota.isNearLimit)
        assertFalse(quota.isOverLimit)
    }
}

class UserRoleTest {

    @Test
    fun shouldHaveCorrectRoles() {
        val roles = UserRole.entries

        assertEquals(3, roles.size)
        assertTrue(roles.contains(UserRole.ADMIN))
        assertTrue(roles.contains(UserRole.USER))
        assertTrue(roles.contains(UserRole.GUEST))
    }
}

class VisibilityTest {

    @Test
    fun shouldHaveCorrectVisibilityOptions() {
        val options = Visibility.entries

        assertEquals(3, options.size)
        assertTrue(options.contains(Visibility.PRIVATE))
        assertTrue(options.contains(Visibility.SHARED))
        assertTrue(options.contains(Visibility.PUBLIC))
    }
}

class ItemTypeTest {

    @Test
    fun shouldHaveCorrectItemTypes() {
        val types = ItemType.entries

        assertEquals(2, types.size)
        assertTrue(types.contains(ItemType.FILE))
        assertTrue(types.contains(ItemType.FOLDER))
    }
}
