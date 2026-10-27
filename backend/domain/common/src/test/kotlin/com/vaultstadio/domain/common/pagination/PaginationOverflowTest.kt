package com.vaultstadio.domain.common.pagination

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PaginationOverflowTest {
    @Test fun pageAfterIntMaxDoesNotWrapOrClaimMoreAtTheEnd() {
        val result = PagedResult(listOf("last"), 2147483648L, Int.MAX_VALUE, 1)
        assertEquals(2147483648L, result.currentPage)
        assertEquals(2147483648L, result.totalPages)
        assertFalse(result.hasMore)
    }

    @Test fun largestTotalWithUnitPagesRemainsExact() {
        val result = PagedResult(listOf("first"), Long.MAX_VALUE, 0, 1)
        assertEquals(Long.MAX_VALUE, result.totalPages)
        assertTrue(result.hasMore)
    }

    @Test fun ceilingDoesNotOverflowItsNumerator() {
        assertEquals(3074457345618258603L, PagedResult(emptyList<String>(), Long.MAX_VALUE, 0, 3).totalPages)
    }

    @Test fun exactLargeDivisionDoesNotAddAnotherPage() {
        assertEquals(4611686018427387903L, PagedResult(emptyList<String>(), Long.MAX_VALUE - 1, 0, 2).totalPages)
    }

    @Test fun emptyDatasetBeyondTheEndRetainsRequestedPage() {
        val result = PagedResult(emptyList<String>(), 0, 20, 10)
        assertEquals(3L, result.currentPage)
        assertEquals(0L, result.totalPages)
        assertFalse(result.hasMore)
    }

    @Test fun unalignedOffsetUsesItsContainingPage() {
        val result = PagedResult(listOf("fourth", "fifth"), 8, 3, 2)
        assertEquals(2L, result.currentPage)
        assertEquals(4L, result.totalPages)
        assertTrue(result.hasMore)
    }

    @Test fun exactEndHasNoAdditionalRows() {
        val result = PagedResult(emptyList<String>(), 8, 8, 2)
        assertEquals(5L, result.currentPage)
        assertEquals(4L, result.totalPages)
        assertFalse(result.hasMore)
    }

    @Test fun legacyNonpositiveProtocolLimitKeepsItsConvention() {
        val result = PagedResult(emptyList<String>(), 8, 0, -1)
        assertEquals(1L, result.currentPage)
        assertEquals(0L, result.totalPages)
    }
}
