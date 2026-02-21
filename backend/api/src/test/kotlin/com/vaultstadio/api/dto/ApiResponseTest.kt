/**
 * Unit tests for ApiResponse, ApiError, and PagedResult.toResponse().
 */

package com.vaultstadio.api.dto

import com.vaultstadio.domain.common.pagination.PagedResult
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
}
