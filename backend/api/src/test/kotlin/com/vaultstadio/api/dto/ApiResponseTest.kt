/**
 * Unit tests for ApiResponse, ApiError, and PagedResult.toResponse().
 */

package com.vaultstadio.api.dto

import com.vaultstadio.domain.activity.model.Activity
import com.vaultstadio.domain.activity.model.ActivityType
import com.vaultstadio.domain.auth.model.User
import com.vaultstadio.domain.auth.model.UserRole
import com.vaultstadio.domain.auth.model.UserStatus
import com.vaultstadio.domain.common.pagination.PagedResult
import com.vaultstadio.domain.share.model.ShareLink
import com.vaultstadio.domain.storage.model.StorageQuota
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
