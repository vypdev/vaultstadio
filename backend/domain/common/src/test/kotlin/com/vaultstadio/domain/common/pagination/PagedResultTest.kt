/**
 * Unit tests for [PagedResult] computed properties.
 */

package com.vaultstadio.domain.common.pagination

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PagedResultTest {

    @Test
    fun hasMoreIsTrueWhenMoreItemsExist() {
        val result = PagedResult(
            items = listOf("a", "b"),
            total = 10,
            offset = 0,
            limit = 2,
        )
        assertTrue(result.hasMore)
        assertEquals(5, result.totalPages)
        assertEquals(1, result.currentPage)
    }

    @Test
    fun hasMoreIsFalseOnLastPage() {
        val result = PagedResult(
            items = listOf("a", "b"),
            total = 4,
            offset = 2,
            limit = 2,
        )
        assertFalse(result.hasMore)
        assertEquals(2, result.totalPages)
        assertEquals(2, result.currentPage)
    }

    @Test
    fun zeroLimitYieldsZeroTotalPagesAndPageOne() {
        val result = PagedResult<String>(
            items = emptyList(),
            total = 0,
            offset = 0,
            limit = 0,
        )
        assertEquals(0, result.totalPages)
        assertEquals(1, result.currentPage)
    }

    @Test
    fun singlePageFullResult() {
        val result = PagedResult(
            items = listOf("a", "b", "c"),
            total = 3,
            offset = 0,
            limit = 10,
        )
        assertFalse(result.hasMore)
        assertEquals(1, result.totalPages)
        assertEquals(1, result.currentPage)
    }
}
