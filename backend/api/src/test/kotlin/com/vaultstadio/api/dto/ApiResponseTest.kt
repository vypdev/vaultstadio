/**
 * Unit tests for ApiResponse, ApiError, and PagedResult.toResponse().
 */

package com.vaultstadio.api.dto

import com.vaultstadio.domain.activity.model.Activity
import com.vaultstadio.domain.activity.model.ActivityType
import com.vaultstadio.domain.auth.model.User
import com.vaultstadio.domain.auth.model.UserInfo
import com.vaultstadio.domain.auth.model.UserRole
import com.vaultstadio.domain.auth.model.UserStatus
import com.vaultstadio.domain.common.pagination.PagedResult
import com.vaultstadio.domain.share.model.ShareLink
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.model.StorageQuota
import com.vaultstadio.domain.storage.model.Visibility
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiResponseTest {

    @Test
    fun `ApiResponse success wraps data`() {
        val response = ApiResponse(success = true, data = "ok")
        assertTrue(response.success)
        assertEquals("ok", response.data)
        assertNull(response.error)
    }

    @Test
    fun `ApiResponse error wraps ApiError`() {
        val err = ApiError(code = "VALIDATION_ERROR", message = "Invalid input", details = mapOf("field" to "name"))
        val response = ApiResponse<String>(success = false, data = null, error = err)
        assertFalse(response.success)
        assertNull(response.data)
        assertEquals("VALIDATION_ERROR", response.error?.code)
        assertEquals("Invalid input", response.error?.message)
        assertEquals("name", response.error?.details?.get("field"))
    }

    @Test
    fun `ApiError without details`() {
        val err = ApiError(code = "NOT_FOUND", message = "Item not found")
        assertEquals("NOT_FOUND", err.code)
        assertEquals("Item not found", err.message)
        assertNull(err.details)
    }
}

class PagedResultToResponseTest {

    @Test
    fun `toResponse maps PagedResult to PaginatedResponse`() {
        val paged = PagedResult<Int>(
            items = listOf(1, 2, 3),
            total = 10L,
            offset = 0,
            limit = 5,
        )
        val response: PaginatedResponse<Int> = paged.toResponse()
        assertEquals(3, response.items.size)
        assertEquals(10L, response.total)
        assertEquals(1, response.page)
        assertEquals(5, response.pageSize)
        assertTrue(response.totalPages >= 1)
        assertTrue(response.hasMore)
    }

    @Test
    fun `toResponse with empty items and hasMore false`() {
        val paged = PagedResult<Int>(
            items = emptyList(),
            total = 0L,
            offset = 0,
            limit = 10,
        )
        val response: PaginatedResponse<Int> = paged.toResponse()
        assertTrue(response.items.isEmpty())
        assertEquals(0L, response.total)
        assertEquals(0, response.totalPages)
        assertFalse(response.hasMore)
    }

    @Test
    fun `toResponse with partial page sets hasMore true and totalPages`() {
        val paged = PagedResult<Int>(
            items = listOf(1, 2, 3, 4, 5),
            total = 25L,
            offset = 0,
            limit = 5,
        )
        val response: PaginatedResponse<Int> = paged.toResponse()
        assertEquals(5, response.items.size)
        assertEquals(25L, response.total)
        assertEquals(5, response.totalPages)
        assertTrue(response.hasMore)
        assertEquals(1, response.page)
        assertEquals(5, response.pageSize)
    }

    @Test
    fun `toResponse with limit zero yields zero totalPages and page one`() {
        val paged = PagedResult<String>(
            items = emptyList(),
            total = 0L,
            offset = 0,
            limit = 0,
        )
        val response: PaginatedResponse<String> = paged.toResponse()
        assertEquals(0, response.totalPages)
        assertEquals(1, response.page)
        assertFalse(response.hasMore)
    }
}

class ShareLinkToResponseTest {

    private val testInstant = Instant.fromEpochMilliseconds(0L)

    @Test
    fun `toResponse builds url from baseUrl and token`() {
        val share = ShareLink(
            id = "share-1",
            itemId = "item-1",
            token = "abc123",
            createdBy = "user-1",
            expiresAt = testInstant,
            password = null,
            maxDownloads = 5,
            downloadCount = 0,
            isActive = true,
            createdAt = testInstant,
            sharedWithUsers = emptyList(),
        )
        val response = share.toResponse("https://api.example.com")
        assertEquals("share-1", response.id)
        assertEquals("item-1", response.itemId)
        assertEquals("abc123", response.token)
        assertEquals("https://api.example.com/share/abc123", response.url)
        assertEquals(5, response.maxDownloads)
        assertEquals(0, response.downloadCount)
        assertFalse(response.hasPassword)
    }

    @Test
    fun `toResponse sets hasPassword true when password is not null`() {
        val share = ShareLink(
            id = "s2",
            itemId = "item-2",
            token = "tok",
            createdBy = "user-1",
            password = "secret",
            createdAt = testInstant,
        )
        val response = share.toResponse("https://api.test")
        assertTrue(response.hasPassword)
        assertEquals("https://api.test/share/tok", response.url)
    }
}

class StorageItemToResponseTest {

    private val testInstant = Instant.fromEpochMilliseconds(0L)

    @Test
    fun `toResponse maps item to StorageItemResponse`() {
        val item = StorageItem(
            id = "item-1",
            name = "doc.pdf",
            path = "/doc.pdf",
            type = ItemType.FILE,
            parentId = "parent-1",
            ownerId = "user-1",
            size = 1024L,
            mimeType = "application/pdf",
            visibility = Visibility.PRIVATE,
            isStarred = true,
            isTrashed = false,
            createdAt = testInstant,
            updatedAt = testInstant,
        )
        val response = item.toResponse()
        assertEquals("item-1", response.id)
        assertEquals("doc.pdf", response.name)
        assertEquals("/doc.pdf", response.path)
        assertEquals(ItemType.FILE, response.type)
        assertEquals("parent-1", response.parentId)
        assertEquals(1024L, response.size)
        assertEquals("application/pdf", response.mimeType)
        assertEquals(Visibility.PRIVATE, response.visibility)
        assertTrue(response.isStarred)
        assertFalse(response.isTrashed)
        assertEquals(testInstant, response.createdAt)
        assertEquals(testInstant, response.updatedAt)
        assertNull(response.metadata)
    }

    @Test
    fun `toResponse with metadata passes metadata to response`() {
        val item = StorageItem(
            id = "f1",
            name = "photo.jpg",
            path = "/photo.jpg",
            type = ItemType.FILE,
            parentId = null,
            ownerId = "user-1",
            size = 0,
            createdAt = testInstant,
            updatedAt = testInstant,
        )
        val metadata = mapOf("camera" to "Canon", "width" to "1920")
        val response = item.toResponse(metadata = metadata)
        assertEquals(metadata, response.metadata)
        assertEquals("f1", response.id)
    }
}

class StorageQuotaToResponseTest {

    @Test
    fun `toResponse maps all fields`() {
        val quota = StorageQuota(
            userId = "user-1",
            usedBytes = 1024L,
            quotaBytes = 10_240L,
            fileCount = 5,
            folderCount = 2,
        )
        val response = quota.toResponse()
        assertEquals(1024L, response.usedBytes)
        assertEquals(10_240L, response.quotaBytes)
        assertEquals(5, response.fileCount)
        assertEquals(2, response.folderCount)
        assertTrue(response.usagePercentage > 0)
        assertEquals(9216L, response.remainingBytes)
    }

    @Test
    fun `toResponse with null quotaBytes`() {
        val quota = StorageQuota(
            userId = "u2",
            usedBytes = 0,
            quotaBytes = null,
            fileCount = 0,
            folderCount = 0,
        )
        val response = quota.toResponse()
        assertNull(response.quotaBytes)
        assertEquals(0.0, response.usagePercentage)
        assertNull(response.remainingBytes)
    }
}

class ActivityToResponseTest {

    private val testInstant = Instant.fromEpochMilliseconds(0L)

    @Test
    fun `toResponse maps all fields`() {
        val activity = Activity(
            id = "act-1",
            type = ActivityType.FILE_UPLOADED,
            userId = "user-1",
            itemId = "item-1",
            itemPath = "/path",
            details = "uploaded file.pdf",
            createdAt = testInstant,
        )
        val response = activity.toResponse()
        assertEquals("act-1", response.id)
        assertEquals(ActivityType.FILE_UPLOADED, response.type)
        assertEquals("user-1", response.userId)
        assertEquals("item-1", response.itemId)
        assertEquals("/path", response.itemPath)
        assertEquals("uploaded file.pdf", response.details)
        assertEquals(testInstant, response.createdAt)
    }
}

class UserInfoToResponseTest {

    private val testInstant = Instant.fromEpochMilliseconds(0L)

    @Test
    fun `toResponse maps UserInfo to UserResponse`() {
        val info = UserInfo(
            id = "info-1",
            email = "info@example.com",
            username = "infouser",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            quotaBytes = 5_000L,
            avatarUrl = "https://example.com/avatar.jpg",
            createdAt = testInstant,
        )
        val response = info.toResponse()
        assertEquals("info-1", response.id)
        assertEquals("info@example.com", response.email)
        assertEquals("infouser", response.username)
        assertEquals(UserRole.USER, response.role)
        assertEquals(UserStatus.ACTIVE, response.status)
        assertEquals("https://example.com/avatar.jpg", response.avatarUrl)
        assertEquals(testInstant, response.createdAt)
    }

    @Test
    fun `toResponse with null avatarUrl`() {
        val info = UserInfo(
            id = "u2",
            email = "u2@test.com",
            username = "u2",
            role = UserRole.ADMIN,
            status = UserStatus.ACTIVE,
            quotaBytes = null,
            avatarUrl = null,
            createdAt = testInstant,
        )
        val response = info.toResponse()
        assertNull(response.avatarUrl)
        assertEquals("u2", response.id)
    }
}

class UserToResponseTest {

    private val testInstant = Instant.fromEpochMilliseconds(0L)

    @Test
    fun `toResponse maps user to UserResponse`() {
        val user = User(
            id = "user-1",
            email = "test@example.com",
            username = "testuser",
            passwordHash = "hash",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            avatarUrl = null,
            createdAt = testInstant,
            updatedAt = testInstant,
        )
        val response = user.toResponse()
        assertEquals("user-1", response.id)
        assertEquals("test@example.com", response.email)
        assertEquals("testuser", response.username)
        assertEquals(UserRole.USER, response.role)
        assertEquals(UserStatus.ACTIVE, response.status)
        assertEquals(testInstant, response.createdAt)
    }
}

class UserToAdminResponseTest {

    private val testInstant = Instant.fromEpochMilliseconds(0L)

    @Test
    fun `toAdminResponse maps user to AdminUserResponse with usedBytes`() {
        val user = User(
            id = "admin-user-1",
            email = "admin@example.com",
            username = "adminuser",
            passwordHash = "hash",
            role = UserRole.ADMIN,
            status = UserStatus.ACTIVE,
            quotaBytes = 1_000_000L,
            avatarUrl = "https://example.com/avatar.png",
            lastLoginAt = testInstant,
            createdAt = testInstant,
            updatedAt = testInstant,
        )
        val response = user.toAdminResponse(usedBytes = 500_000L)
        assertEquals("admin-user-1", response.id)
        assertEquals("admin@example.com", response.email)
        assertEquals("adminuser", response.username)
        assertEquals(UserRole.ADMIN, response.role)
        assertEquals(UserStatus.ACTIVE, response.status)
        assertEquals("https://example.com/avatar.png", response.avatarUrl)
        assertEquals(1_000_000L, response.quotaBytes)
        assertEquals(500_000L, response.usedBytes)
        assertEquals(testInstant, response.createdAt)
        assertEquals(testInstant, response.lastLoginAt)
    }

    @Test
    fun `toAdminResponse with default usedBytes yields zero`() {
        val user = User(
            id = "u2",
            email = "u2@test.com",
            username = "u2",
            passwordHash = "h",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            quotaBytes = null,
            createdAt = testInstant,
            updatedAt = testInstant,
        )
        val response = user.toAdminResponse()
        assertEquals(0L, response.usedBytes)
        assertNull(response.quotaBytes)
        assertNull(response.lastLoginAt)
    }
}

class CreateShareRequestTest {

    @Test
    fun `holds itemId and optional fields`() {
        val req = CreateShareRequest(
            itemId = "item-1",
            expirationDays = 7,
            password = "secret",
            maxDownloads = 10,
        )
        assertEquals("item-1", req.itemId)
        assertEquals(7, req.expirationDays)
        assertEquals("secret", req.password)
        assertEquals(10, req.maxDownloads)
    }

    @Test
    fun `defaults optional fields to null`() {
        val req = CreateShareRequest(itemId = "item-2")
        assertEquals("item-2", req.itemId)
        assertNull(req.expirationDays)
        assertNull(req.password)
        assertNull(req.maxDownloads)
    }
}

class AccessShareRequestTest {

    @Test
    fun `holds optional password`() {
        val req = AccessShareRequest(password = "pwd")
        assertEquals("pwd", req.password)
    }

    @Test
    fun `defaults password to null`() {
        val req = AccessShareRequest()
        assertNull(req.password)
    }
}

class SearchRequestTest {

    @Test
    fun `holds query and optional params`() {
        val req = SearchRequest(
            query = "test",
            type = ItemType.FILE,
            mimeType = "image/*",
            limit = 20,
            offset = 10,
        )
        assertEquals("test", req.query)
        assertEquals(ItemType.FILE, req.type)
        assertEquals("image/*", req.mimeType)
        assertEquals(20, req.limit)
        assertEquals(10, req.offset)
    }

    @Test
    fun `defaults type mimeType offset and limit`() {
        val req = SearchRequest(query = "q")
        assertEquals("q", req.query)
        assertNull(req.type)
        assertNull(req.mimeType)
        assertEquals(50, req.limit)
        assertEquals(0, req.offset)
    }
}

class PluginInfoResponseTest {

    @Test
    fun `holds all plugin info fields`() {
        val res = PluginInfoResponse(
            id = "plugin-1",
            name = "Image Metadata",
            version = "1.0.0",
            description = "Extracts EXIF",
            author = "VaultStadio",
            isEnabled = true,
            state = "loaded",
        )
        assertEquals("plugin-1", res.id)
        assertEquals("Image Metadata", res.name)
        assertEquals("1.0.0", res.version)
        assertTrue(res.isEnabled)
        assertEquals("loaded", res.state)
    }
}

class CreateFolderRequestTest {

    @Test
    fun `holds name and optional parentId`() {
        val req = CreateFolderRequest(name = "Documents", parentId = "parent-1")
        assertEquals("Documents", req.name)
        assertEquals("parent-1", req.parentId)
    }

    @Test
    fun `defaults parentId to null`() {
        val req = CreateFolderRequest(name = "New Folder")
        assertEquals("New Folder", req.name)
        assertNull(req.parentId)
    }
}

class RenameRequestTest {

    @Test
    fun `holds name`() {
        val req = RenameRequest(name = "renamed.txt")
        assertEquals("renamed.txt", req.name)
    }
}

class MoveRequestTest {

    @Test
    fun `holds destinationId and optional newName`() {
        val req = MoveRequest(destinationId = "folder-1", newName = "moved.pdf")
        assertEquals("folder-1", req.destinationId)
        assertEquals("moved.pdf", req.newName)
    }

    @Test
    fun `defaults newName to null`() {
        val req = MoveRequest(destinationId = null)
        assertNull(req.destinationId)
        assertNull(req.newName)
    }
}

class CopyRequestTest {

    @Test
    fun `holds destinationId and optional newName`() {
        val req = CopyRequest(destinationId = "target-folder", newName = "copy.pdf")
        assertEquals("target-folder", req.destinationId)
        assertEquals("copy.pdf", req.newName)
    }

    @Test
    fun `defaults newName to null`() {
        val req = CopyRequest(destinationId = "dest")
        assertEquals("dest", req.destinationId)
        assertNull(req.newName)
    }
}
