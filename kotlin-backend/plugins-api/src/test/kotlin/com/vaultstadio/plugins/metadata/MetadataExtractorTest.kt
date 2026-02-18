/**
 * VaultStadio Metadata Extractor API Tests
 *
 * Unit tests for MetadataKeys, ExtractedMetadata, MetadataExtractorConfig, and MetadataExtractionHook.
 */

package com.vaultstadio.plugins.metadata

import com.vaultstadio.core.domain.model.ItemType
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.plugins.hooks.MetadataExtractionHook
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MetadataExtractorTest {

    @Nested
    inner class MetadataKeysTests {

        @Test
        fun `common keys are defined`() {
            assertEquals("title", MetadataKeys.TITLE)
            assertEquals("description", MetadataKeys.DESCRIPTION)
            assertEquals("author", MetadataKeys.AUTHOR)
        }

        @Test
        fun `media keys are defined`() {
            assertEquals("duration", MetadataKeys.DURATION)
            assertEquals("width", MetadataKeys.WIDTH)
            assertEquals("height", MetadataKeys.HEIGHT)
        }

        @Test
        fun `image keys are defined`() {
            assertEquals("camera_make", MetadataKeys.CAMERA_MAKE)
            assertEquals("iso", MetadataKeys.ISO)
        }

        @Test
        fun `AI keys are defined`() {
            assertEquals("ai_tags", MetadataKeys.AI_TAGS)
            assertEquals("classification", MetadataKeys.CLASSIFICATION)
        }
    }

    @Nested
    inner class ExtractedMetadataTests {

        @Test
        fun `ExtractedMetadata holds values and optional thumbnail and text`() {
            val m = ExtractedMetadata(
                values = mapOf("title" to "Doc"),
                thumbnail = byteArrayOf(1, 2, 3),
                textContent = "full text",
                warnings = listOf("w1"),
            )
            assertEquals(mapOf("title" to "Doc"), m.values)
            assertTrue(m.thumbnail?.contentEquals(byteArrayOf(1, 2, 3)) == true)
            assertEquals("full text", m.textContent)
            assertEquals(listOf("w1"), m.warnings)
        }

        @Test
        fun `ExtractedMetadata equals and hashCode`() {
            val a = ExtractedMetadata(values = mapOf("k" to "v"))
            val b = ExtractedMetadata(values = mapOf("k" to "v"))
            val c = ExtractedMetadata(values = mapOf("k" to "v2"))
            assertEquals(a, b)
            assertEquals(a.hashCode(), b.hashCode())
            assertFalse(a == c)
        }
    }

    @Nested
    inner class MetadataExtractorConfigTests {

        @Test
        fun `default config values`() {
            val c = MetadataExtractorConfig()
            assertTrue(c.autoExtract)
            assertEquals(0L, c.maxFileSize)
            assertFalse(c.extractTextContent)
            assertFalse(c.generateThumbnails)
            assertEquals(256, c.thumbnailWidth)
            assertEquals(256, c.thumbnailHeight)
        }

        @Test
        fun `config with custom values`() {
            val c = MetadataExtractorConfig(
                autoExtract = false,
                maxFileSize = 10_000_000,
                extractTextContent = true,
                generateThumbnails = true,
                thumbnailWidth = 128,
                thumbnailHeight = 128,
            )
            assertFalse(c.autoExtract)
            assertEquals(10_000_000L, c.maxFileSize)
            assertTrue(c.extractTextContent)
            assertTrue(c.generateThumbnails)
            assertEquals(128, c.thumbnailWidth)
        }
    }

    @Nested
    inner class MetadataExtractionHookTests {

        @Test
        fun `stub extractor returns map from extractMetadata`() = runTest {
            val hook = object : MetadataExtractionHook {
                override suspend fun extractMetadata(
                    item: StorageItem,
                    stream: InputStream,
                ): Map<String, String> = mapOf("extracted" to "value")
                override fun getSupportedMimeTypes(): Set<String> = setOf("image/jpeg")
            }
            val item = StorageItem(
                id = "id",
                name = "x.jpg",
                path = "/x.jpg",
                type = ItemType.FILE,
                ownerId = "u1",
                size = 0,
                mimeType = "image/jpeg",
                createdAt = kotlinx.datetime.Instant.DISTANT_PAST,
                updatedAt = kotlinx.datetime.Instant.DISTANT_PAST,
                parentId = null,
                storageKey = null,
            )
            val stream = ByteArrayInputStream(ByteArray(0))
            val result = hook.extractMetadata(item, stream)
            assertEquals(mapOf("extracted" to "value"), result)
            assertEquals(setOf("image/jpeg"), hook.getSupportedMimeTypes())
        }
    }
}
