/**
 * VaultStadio Advanced Search Dialog Tests
 */

package com.vaultstadio.app.ui.components.dialogs

import com.vaultstadio.app.domain.model.AdvancedSearchRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdvancedSearchDialogTest {

    @Test
    fun testFileTypeFilterValues() {
        val filters = FileTypeFilter.entries
        assertEquals(8, filters.size)

        // Check each filter has mime types
        filters.forEach { filter ->
            assertTrue(filter.mimeTypes.isNotEmpty(), "Filter ${filter.name} should have mime types")
        }
    }

    @Test
    fun testFileTypeFilterImages() {
        val imageFilter = FileTypeFilter.IMAGES
        assertTrue(imageFilter.mimeTypes.contains("image/jpeg"))
        assertTrue(imageFilter.mimeTypes.contains("image/png"))
        assertTrue(imageFilter.mimeTypes.contains("image/gif"))
        assertTrue(imageFilter.mimeTypes.contains("image/webp"))
    }

    @Test
    fun testFileTypeFilterVideos() {
        val videoFilter = FileTypeFilter.VIDEOS
        assertTrue(videoFilter.mimeTypes.contains("video/mp4"))
        assertTrue(videoFilter.mimeTypes.contains("video/webm"))
    }

    @Test
    fun testFileTypeFilterDocuments() {
        val docFilter = FileTypeFilter.DOCUMENTS
        assertTrue(docFilter.mimeTypes.contains("application/pdf"))
    }

    @Test
    fun testDateRangeFilterValues() {
        val filters = DateRangeFilter.entries
        assertEquals(6, filters.size)
    }

    @Test
    fun testDateRangeFilterAny() {
        assertNull(DateRangeFilter.ANY.days)
    }

    @Test
    fun testDateRangeFilterToday() {
        assertEquals(1, DateRangeFilter.TODAY.days)
    }

    @Test
    fun testDateRangeFilterWeek() {
        assertEquals(7, DateRangeFilter.WEEK.days)
    }

    @Test
    fun testDateRangeFilterMonth() {
        assertEquals(30, DateRangeFilter.MONTH.days)
    }

    @Test
    fun testDateRangeFilterQuarter() {
        assertEquals(90, DateRangeFilter.QUARTER.days)
    }

    @Test
    fun testDateRangeFilterYear() {
        assertEquals(365, DateRangeFilter.YEAR.days)
    }

    @Test
    fun testSizeRangeFilterValues() {
        val filters = SizeRangeFilter.entries
        assertEquals(6, filters.size)
    }

    @Test
    fun testSizeRangeFilterAny() {
        assertNull(SizeRangeFilter.ANY.minBytes)
        assertNull(SizeRangeFilter.ANY.maxBytes)
    }

    @Test
    fun testSizeRangeFilterTiny() {
        assertNull(SizeRangeFilter.TINY.minBytes)
        assertNotNull(SizeRangeFilter.TINY.maxBytes)
    }

    @Test
    fun testSizeRangeFilterHuge() {
        assertNotNull(SizeRangeFilter.HUGE.minBytes)
        assertNull(SizeRangeFilter.HUGE.maxBytes)
    }

    @Test
    fun testSizeRangeFilterMedium() {
        assertNotNull(SizeRangeFilter.MEDIUM.minBytes)
        assertNotNull(SizeRangeFilter.MEDIUM.maxBytes)
    }

    @Test
    fun testAdvancedSearchRequestModel() {
        val request = AdvancedSearchRequest(
            query = "test query",
            searchContent = true,
            fileTypes = listOf("image/jpeg", "image/png"),
            minSize = 1024,
            maxSize = 10 * 1024 * 1024,
            fromDate = null,
            toDate = null,
            limit = 50,
            offset = 0,
        )

        assertEquals("test query", request.query)
        assertTrue(request.searchContent)
        assertEquals(2, request.fileTypes?.size)
        assertEquals(1024, request.minSize)
    }

    @Test
    fun testAdvancedSearchRequestDefaults() {
        val request = AdvancedSearchRequest(query = "simple query")

        assertEquals("simple query", request.query)
        assertEquals(false, request.searchContent)
        assertEquals(null, request.fileTypes)
        assertEquals(null, request.minSize)
        assertEquals(null, request.maxSize)
        assertEquals(50, request.limit)
        assertEquals(0, request.offset)
    }
}
