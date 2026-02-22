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

    @Test
    fun `ApiError with empty details map`() {
        val err = ApiError(code = "BAD_REQUEST", message = "Invalid", details = emptyMap())
        assertEquals("BAD_REQUEST", err.code)
        assertEquals("Invalid", err.message)
        assertTrue(err.details?.isEmpty() == true)
    }

    @Test
    fun `ApiResponse success with default null data`() {
        val response = ApiResponse<Unit?>(success = true)
        assertTrue(response.success)
        assertNull(response.data)
        assertNull(response.error)
    }
}

class PaginatedResponseTest {

    @Test
    fun `PaginatedResponse construction`() {
        val r = PaginatedResponse(
            items = listOf("a", "b"),
            total = 10L,
            page = 1,
            pageSize = 5,
            totalPages = 2,
            hasMore = true,
        )
        assertEquals(2, r.items.size)
        assertEquals(10L, r.total)
        assertEquals(1, r.page)
        assertEquals(5, r.pageSize)
        assertEquals(2, r.totalPages)
        assertTrue(r.hasMore)
    }

    @Test
    fun `PaginatedResponse empty items and hasMore false`() {
        val r = PaginatedResponse<Int>(
            items = emptyList(),
            total = 0L,
            page = 1,
            pageSize = 10,
            totalPages = 0,
            hasMore = false,
        )
        assertTrue(r.items.isEmpty())
        assertEquals(0, r.totalPages)
        assertFalse(r.hasMore)
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

class ShareResponseTest {

    private val testInstant = Instant.fromEpochMilliseconds(0L)

    @Test
    fun `ShareResponse construction and default sharedWithUsers`() {
        val r = ShareResponse(
            id = "share-1",
            itemId = "item-1",
            token = "tkn",
            url = "https://api.example.com/share/tkn",
            expiresAt = testInstant,
            hasPassword = false,
            maxDownloads = 10,
            downloadCount = 0,
            isActive = true,
            createdAt = testInstant,
            createdBy = "user-1",
        )
        assertEquals("share-1", r.id)
        assertEquals("item-1", r.itemId)
        assertEquals(10, r.maxDownloads)
        assertTrue(r.sharedWithUsers.isEmpty())
    }

    @Test
    fun `ShareResponse with sharedWithUsers`() {
        val r = ShareResponse(
            id = "s2",
            itemId = "i2",
            token = "t",
            url = "https://api.test/share/t",
            expiresAt = null,
            hasPassword = true,
            maxDownloads = null,
            downloadCount = 1,
            isActive = true,
            createdAt = testInstant,
            createdBy = "admin",
            sharedWithUsers = listOf("user-a", "user-b"),
        )
        assertEquals(2, r.sharedWithUsers.size)
        assertEquals("user-a", r.sharedWithUsers[0])
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

class StorageItemResponseTest {

    private val testInstant = Instant.fromEpochMilliseconds(0L)

    @Test
    fun `StorageItemResponse construction and default metadata`() {
        val r = StorageItemResponse(
            id = "item-1",
            name = "doc.pdf",
            path = "/doc.pdf",
            type = ItemType.FILE,
            parentId = "parent-1",
            size = 1024L,
            mimeType = "application/pdf",
            visibility = Visibility.PRIVATE,
            isStarred = true,
            isTrashed = false,
            createdAt = testInstant,
            updatedAt = testInstant,
        )
        assertEquals("item-1", r.id)
        assertEquals("doc.pdf", r.name)
        assertEquals(ItemType.FILE, r.type)
        assertEquals(1024L, r.size)
        assertNull(r.metadata)
    }

    @Test
    fun `StorageItemResponse with metadata`() {
        val meta = mapOf("key" to "value")
        val r = StorageItemResponse(
            id = "f1",
            name = "f",
            path = "/f",
            type = ItemType.FILE,
            parentId = null,
            size = 0L,
            mimeType = null,
            visibility = Visibility.PRIVATE,
            isStarred = false,
            isTrashed = false,
            createdAt = testInstant,
            updatedAt = testInstant,
            metadata = meta,
        )
        assertEquals(meta, r.metadata)
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

class QuotaResponseTest {

    private val testInstant = Instant.fromEpochMilliseconds(0L)

    @Test
    fun `QuotaResponse construction`() {
        val r = QuotaResponse(
            usedBytes = 1024L,
            quotaBytes = 10_240L,
            usagePercentage = 10.0,
            fileCount = 5L,
            folderCount = 2L,
            remainingBytes = 9216L,
        )
        assertEquals(1024L, r.usedBytes)
        assertEquals(10_240L, r.quotaBytes)
        assertEquals(10.0, r.usagePercentage)
        assertEquals(5L, r.fileCount)
        assertEquals(2L, r.folderCount)
        assertEquals(9216L, r.remainingBytes)
    }

    @Test
    fun `QuotaResponse with null quotaBytes and remainingBytes`() {
        val r = QuotaResponse(
            usedBytes = 0L,
            quotaBytes = null,
            usagePercentage = 0.0,
            fileCount = 0L,
            folderCount = 0L,
            remainingBytes = null,
        )
        assertNull(r.quotaBytes)
        assertNull(r.remainingBytes)
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

class ActivityResponseTest {

    private val testInstant = Instant.fromEpochMilliseconds(0L)

    @Test
    fun `ActivityResponse construction`() {
        val r = ActivityResponse(
            id = "act-1",
            type = ActivityType.FILE_UPLOADED,
            userId = "user-1",
            itemId = "item-1",
            itemPath = "/path",
            details = "{}",
            createdAt = testInstant,
        )
        assertEquals("act-1", r.id)
        assertEquals(ActivityType.FILE_UPLOADED, r.type)
        assertEquals("user-1", r.userId)
        assertEquals("item-1", r.itemId)
        assertEquals("/path", r.itemPath)
        assertEquals("{}", r.details)
        assertEquals(testInstant, r.createdAt)
    }

    @Test
    fun `ActivityResponse with null optional fields`() {
        val r = ActivityResponse(
            id = "a2",
            type = ActivityType.USER_LOGOUT,
            userId = null,
            itemId = null,
            itemPath = null,
            details = null,
            createdAt = testInstant,
        )
        assertNull(r.userId)
        assertNull(r.itemId)
        assertNull(r.details)
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

class UserResponseTest {

    private val testInstant = Instant.fromEpochMilliseconds(0L)

    @Test
    fun `UserResponse construction`() {
        val r = UserResponse(
            id = "user-1",
            email = "a@b.com",
            username = "alice",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            avatarUrl = null,
            createdAt = testInstant,
        )
        assertEquals("user-1", r.id)
        assertEquals("a@b.com", r.email)
        assertEquals("alice", r.username)
        assertEquals(UserRole.USER, r.role)
        assertEquals(UserStatus.ACTIVE, r.status)
        assertNull(r.avatarUrl)
    }

    @Test
    fun `UserResponse with avatarUrl`() {
        val r = UserResponse(
            id = "u2",
            email = "b@c.com",
            username = "bob",
            role = UserRole.ADMIN,
            status = UserStatus.ACTIVE,
            avatarUrl = "https://cdn.example/av.png",
            createdAt = testInstant,
        )
        assertEquals("https://cdn.example/av.png", r.avatarUrl)
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

class AdminUserResponseTest {

    private val testInstant = Instant.fromEpochMilliseconds(0L)

    @Test
    fun `AdminUserResponse construction`() {
        val r = AdminUserResponse(
            id = "au1",
            email = "admin@example.com",
            username = "admin",
            role = UserRole.ADMIN,
            status = UserStatus.ACTIVE,
            avatarUrl = null,
            quotaBytes = 1_000_000L,
            usedBytes = 500_000L,
            createdAt = testInstant,
            lastLoginAt = testInstant,
        )
        assertEquals("au1", r.id)
        assertEquals("admin@example.com", r.email)
        assertEquals(UserRole.ADMIN, r.role)
        assertEquals(1_000_000L, r.quotaBytes)
        assertEquals(500_000L, r.usedBytes)
    }

    @Test
    fun `AdminUserResponse with null optional fields`() {
        val r = AdminUserResponse(
            id = "au2",
            email = "u@t.com",
            username = "user",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            avatarUrl = null,
            quotaBytes = null,
            usedBytes = 0L,
            createdAt = testInstant,
            lastLoginAt = null,
        )
        assertNull(r.avatarUrl)
        assertNull(r.quotaBytes)
        assertNull(r.lastLoginAt)
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

    @Test
    fun `holds itemId and expirationDays only`() {
        val req = CreateShareRequest(itemId = "item-3", expirationDays = 30)
        assertEquals("item-3", req.itemId)
        assertEquals(30, req.expirationDays)
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
        assertEquals("Extracts EXIF", res.description)
        assertEquals("VaultStadio", res.author)
        assertTrue(res.isEnabled)
        assertEquals("loaded", res.state)
    }
}

class PluginConfigResponseTest {

    @Test
    fun `holds pluginId and config map`() {
        val config = mapOf<String, Any?>(
            "enabled" to true,
            "maxSize" to 1024,
            "path" to "/data",
        )
        val res = PluginConfigResponse(pluginId = "plugin-1", config = config)
        assertEquals("plugin-1", res.pluginId)
        assertEquals(3, res.config.size)
        assertEquals(true, res.config["enabled"])
        assertEquals(1024, res.config["maxSize"])
        assertEquals("/data", res.config["path"])
    }

    @Test
    fun `holds empty config`() {
        val res = PluginConfigResponse(pluginId = "p2", config = emptyMap())
        assertEquals("p2", res.pluginId)
        assertTrue(res.config.isEmpty())
    }
}

class RegisterRequestTest {

    @Test
    fun `holds email username password`() {
        val req = RegisterRequest(
            email = "user@example.com",
            username = "newuser",
            password = "secret123",
        )
        assertEquals("user@example.com", req.email)
        assertEquals("newuser", req.username)
        assertEquals("secret123", req.password)
    }
}

class LoginRequestTest {

    @Test
    fun `holds email and password`() {
        val req = LoginRequest(email = "login@test.com", password = "pwd")
        assertEquals("login@test.com", req.email)
        assertEquals("pwd", req.password)
    }
}

class RefreshRequestTest {

    @Test
    fun `holds refreshToken`() {
        val req = RefreshRequest(refreshToken = "rt-abc123")
        assertEquals("rt-abc123", req.refreshToken)
    }
}

class LoginResponseTest {

    private val testInstant = Instant.fromEpochMilliseconds(0L)

    @Test
    fun `holds user token refreshToken expiresAt`() {
        val userResponse = User(
            id = "u1",
            email = "a@b.com",
            username = "u1",
            passwordHash = "h",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            avatarUrl = null,
            createdAt = testInstant,
            updatedAt = testInstant,
        ).toResponse()
        val res = LoginResponse(
            user = userResponse,
            token = "jwt-token",
            refreshToken = "refresh-token",
            expiresAt = testInstant,
        )
        assertEquals("u1", res.user.id)
        assertEquals("jwt-token", res.token)
        assertEquals("refresh-token", res.refreshToken)
        assertEquals(testInstant, res.expiresAt)
    }
}

class RefreshResponseTest {

    private val testInstant = Instant.fromEpochMilliseconds(0L)

    @Test
    fun `holds user token refreshToken expiresAt`() {
        val userResponse = User(
            id = "u2",
            email = "x@y.com",
            username = "u2",
            passwordHash = "h",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            avatarUrl = null,
            createdAt = testInstant,
            updatedAt = testInstant,
        ).toResponse()
        val res = RefreshResponse(
            user = userResponse,
            token = "new-jwt",
            refreshToken = "new-refresh",
            expiresAt = testInstant,
        )
        assertEquals("u2", res.user.id)
        assertEquals("new-jwt", res.token)
        assertEquals("new-refresh", res.refreshToken)
    }
}

class UpdateProfileRequestTest {

    @Test
    fun `holds optional username and avatarUrl`() {
        val req = UpdateProfileRequest(username = "newname", avatarUrl = "https://example.com/av.png")
        assertEquals("newname", req.username)
        assertEquals("https://example.com/av.png", req.avatarUrl)
    }

    @Test
    fun `defaults to null`() {
        val req = UpdateProfileRequest()
        assertNull(req.username)
        assertNull(req.avatarUrl)
    }
}

class ChangePasswordRequestTest {

    @Test
    fun `holds currentPassword and newPassword`() {
        val req = ChangePasswordRequest(currentPassword = "old", newPassword = "new")
        assertEquals("old", req.currentPassword)
        assertEquals("new", req.newPassword)
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

    @Test
    fun `holds name with special characters`() {
        val req = CreateFolderRequest(name = "Folder (2024)", parentId = "root")
        assertEquals("Folder (2024)", req.name)
        assertEquals("root", req.parentId)
    }
}

class RenameRequestTest {

    @Test
    fun `holds name`() {
        val req = RenameRequest(name = "renamed.txt")
        assertEquals("renamed.txt", req.name)
    }

    @Test
    fun `holds name with folder`() {
        val req = RenameRequest(name = "My Documents")
        assertEquals("My Documents", req.name)
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

    @Test
    fun `holds destinationId with default newName null`() {
        val req = MoveRequest(destinationId = "folder-1")
        assertEquals("folder-1", req.destinationId)
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
