/**
 * VaultStadio StorageChart Tests
 *
 * Tests for storage category and chart logic.
 */

package com.vaultstadio.app.ui.components.files

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for StorageCategory used by StorageChart.
 */
class StorageChartTest {

    @Test
    fun `StorageCategory holds correct values`() {
        val category = StorageCategory(
            name = "Images",
            bytes = 1024L * 1024L * 100L,
            color = Color.Blue,
        )

        assertEquals("Images", category.name)
        assertEquals(104857600L, category.bytes)
        assertEquals(Color.Blue, category.color)
    }

    @Test
    fun `StorageCategory equality`() {
        val category1 = StorageCategory("Videos", 500L, Color.Red)
        val category2 = StorageCategory("Videos", 500L, Color.Red)

        assertEquals(category1, category2)
    }

    @Test
    fun `StorageCategory copy`() {
        val original = StorageCategory("Documents", 1000L, Color.Green)
        val modified = original.copy(bytes = 2000L)

        assertEquals("Documents", modified.name)
        assertEquals(2000L, modified.bytes)
        assertEquals(Color.Green, modified.color)
    }

    @Test
    fun `Calculate storage percentage`() {
        val usedBytes = 750L * 1024L * 1024L * 1024L // 750 GB
        val totalBytes = 1000L * 1024L * 1024L * 1024L // 1 TB

        val percentage = (usedBytes.toFloat() / totalBytes.toFloat() * 100).toInt()

        assertEquals(75, percentage)
    }

    @Test
    fun `Storage warning thresholds`() {
        val totalBytes = 1000L

        // Below warning threshold
        val low = 800L
        assertTrue(low.toFloat() / totalBytes < 0.85f)

        // Warning threshold
        val warning = 850L
        assertTrue(warning.toFloat() / totalBytes >= 0.85f)

        // Critical threshold
        val critical = 950L
        assertTrue(critical.toFloat() / totalBytes >= 0.95f)
    }

    @Test
    fun `Zero total bytes handled`() {
        val usedBytes = 100L
        val totalBytes = 0L

        // Should not crash - percentage is 0 when total is 0
        val percentage = if (totalBytes > 0) {
            (usedBytes.toFloat() / totalBytes.toFloat() * 100).toInt()
        } else {
            0
        }

        assertEquals(0, percentage)
    }

    @Test
    fun `Multiple categories sum correctly`() {
        val categories = listOf(
            StorageCategory("Images", 100L, Color.Blue),
            StorageCategory("Videos", 200L, Color.Red),
            StorageCategory("Documents", 150L, Color.Green),
        )

        val totalBytes = categories.sumOf { it.bytes }

        assertEquals(450L, totalBytes)
    }

    @Test
    fun `Category percentage calculation`() {
        val categories = listOf(
            StorageCategory("Images", 250L, Color.Blue),
            StorageCategory("Videos", 500L, Color.Red),
            StorageCategory("Other", 250L, Color.Gray),
        )

        val totalBytes = categories.sumOf { it.bytes }

        val imagesPercentage = (categories[0].bytes.toFloat() / totalBytes * 100).toInt()
        val videosPercentage = (categories[1].bytes.toFloat() / totalBytes * 100).toInt()
        val otherPercentage = (categories[2].bytes.toFloat() / totalBytes * 100).toInt()

        assertEquals(25, imagesPercentage)
        assertEquals(50, videosPercentage)
        assertEquals(25, otherPercentage)
    }
}
