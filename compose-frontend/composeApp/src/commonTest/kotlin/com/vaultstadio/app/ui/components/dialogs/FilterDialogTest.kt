/**
 * VaultStadio FilterDialog Tests
 *
 * Tests for filter configuration model.
 */

package com.vaultstadio.app.ui.components.dialogs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for FilterConfig model used by FilterDialog.
 */
class FilterDialogTest {

    @Test
    fun `FilterConfig default values`() {
        val config = FilterConfig()

        assertTrue(config.fileTypes.isEmpty())
        assertEquals(null, config.dateRangeDays)
        assertEquals(null, config.minSizeBytes)
        assertEquals(null, config.maxSizeBytes)
        assertFalse(config.searchInContent)
    }

    @Test
    fun `FilterConfig with file types`() {
        val config = FilterConfig(
            fileTypes = setOf("Images", "Videos"),
        )

        assertEquals(2, config.fileTypes.size)
        assertTrue(config.fileTypes.contains("Images"))
        assertTrue(config.fileTypes.contains("Videos"))
    }

    @Test
    fun `FilterConfig with date range`() {
        val config = FilterConfig(
            dateRangeDays = 7,
        )

        assertEquals(7, config.dateRangeDays)
    }

    @Test
    fun `FilterConfig with size range`() {
        val config = FilterConfig(
            minSizeBytes = 1024L * 1024L,
            maxSizeBytes = 10L * 1024L * 1024L,
        )

        assertEquals(1024L * 1024L, config.minSizeBytes)
        assertEquals(10L * 1024L * 1024L, config.maxSizeBytes)
    }

    @Test
    fun `FilterConfig with search in content`() {
        val config = FilterConfig(
            searchInContent = true,
        )

        assertTrue(config.searchInContent)
    }

    @Test
    fun `FilterConfig full configuration`() {
        val config = FilterConfig(
            fileTypes = setOf("Documents", "Images"),
            dateRangeDays = 30,
            minSizeBytes = 1024L,
            maxSizeBytes = 100L * 1024L * 1024L,
            searchInContent = true,
        )

        assertEquals(2, config.fileTypes.size)
        assertEquals(30, config.dateRangeDays)
        assertEquals(1024L, config.minSizeBytes)
        assertEquals(100L * 1024L * 1024L, config.maxSizeBytes)
        assertTrue(config.searchInContent)
    }

    @Test
    fun `FilterConfig copy with modifications`() {
        val original = FilterConfig(
            fileTypes = setOf("Images"),
            dateRangeDays = 7,
        )

        val modified = original.copy(
            fileTypes = original.fileTypes + "Videos",
            dateRangeDays = 30,
        )

        // Original unchanged
        assertEquals(1, original.fileTypes.size)
        assertEquals(7, original.dateRangeDays)

        // Modified has new values
        assertEquals(2, modified.fileTypes.size)
        assertEquals(30, modified.dateRangeDays)
    }

    @Test
    fun `FilterConfig equality`() {
        val config1 = FilterConfig(
            fileTypes = setOf("Images"),
            dateRangeDays = 7,
        )

        val config2 = FilterConfig(
            fileTypes = setOf("Images"),
            dateRangeDays = 7,
        )

        assertEquals(config1, config2)
    }
}
