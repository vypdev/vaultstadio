/**
 * VaultStadio Plugin Hooks Tests
 *
 * Unit tests for hook interfaces and data classes: ClassificationResult, ContentAnalysisResult,
 * TransformationResult, and hook contracts.
 */

package com.vaultstadio.plugins.hooks

import com.vaultstadio.core.domain.model.ItemType
import com.vaultstadio.core.domain.model.StorageItem
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class HooksTest {

    @Nested
    inner class ClassificationResultTests {

        @Test
        fun `ClassificationResult holds labels and confidence`() {
            val labels = listOf(
                ClassificationLabel("cat", 0.9, "animal"),
                ClassificationLabel("dog", 0.1, "animal"),
            )
            val r = ClassificationResult(labels = labels, confidence = 0.9)
            assertEquals(2, r.labels.size)
            assertEquals(0.9, r.confidence)
            assertEquals("cat", r.labels[0].name)
            assertEquals(0.9, r.labels[0].confidence)
            assertEquals("animal", r.labels[0].category)
        }
    }

    @Nested
    inner class ClassificationLabelTests {

        @Test
        fun `ClassificationLabel category optional`() {
            val l = ClassificationLabel("tag", 0.5, null)
            assertEquals("tag", l.name)
            assertEquals(0.5, l.confidence)
            assertEquals(null, l.category)
        }
    }

    @Nested
    inner class ContentAnalysisResultTests {

        @Test
        fun `ContentAnalysisResult holds text and optional fields`() {
            val r = ContentAnalysisResult(
                text = "Hello world",
                language = "en",
                confidence = 0.95,
                segments = listOf(ContentSegment("Hello", 0, 5, 1.0)),
            )
            assertEquals("Hello world", r.text)
            assertEquals("en", r.language)
            assertEquals(0.95, r.confidence)
            assertEquals(1, r.segments.size)
            assertEquals("Hello", r.segments[0].text)
        }
    }

    @Nested
    inner class ContentSegmentTests {

        @Test
        fun `ContentSegment with defaults`() {
            val s = ContentSegment(text = "seg")
            assertEquals("seg", s.text)
            assertEquals(0L, s.startOffset)
            assertEquals(0L, s.endOffset)
            assertEquals(0.0, s.confidence)
        }
    }

    @Nested
    inner class TransformationResultTests {

        @Test
        fun `TransformationResult holds data and mimeType`() {
            val data = byteArrayOf(1, 2, 3)
            val r = TransformationResult(data = data, mimeType = "image/png", metadata = mapOf("k" to "v"))
            assertTrue(r.data.contentEquals(data))
            assertEquals("image/png", r.mimeType)
            assertEquals(mapOf("k" to "v"), r.metadata)
        }

        @Test
        fun `TransformationResult equals and hashCode`() {
            val a = TransformationResult(byteArrayOf(1), "text/plain")
            val b = TransformationResult(byteArrayOf(1), "text/plain")
            val c = TransformationResult(byteArrayOf(2), "text/plain")
            assertEquals(a, b)
            assertEquals(a.hashCode(), b.hashCode())
            assertFalse(a == c)
        }
    }

    @Nested
    inner class ThumbnailHookTests {

        @Test
        fun `ThumbnailHook implementation returns thumbnail and metadata`() = runTest {
            val hook = object : ThumbnailHook {
                override suspend fun generateThumbnail(
                    item: StorageItem,
                    stream: InputStream,
                ): ByteArray? = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
                override fun getThumbnailMimeType(): String = "image/jpeg"
                override fun getThumbnailMaxSize(): Int = 256
            }
            val item = StorageItem(
                id = "i",
                name = "x.jpg",
                path = "/x.jpg",
                type = ItemType.FILE,
                ownerId = "u",
                size = 0,
                mimeType = "image/jpeg",
                createdAt = kotlinx.datetime.Instant.DISTANT_PAST,
                updatedAt = kotlinx.datetime.Instant.DISTANT_PAST,
                parentId = null,
                storageKey = null,
            )
            val thumb = hook.generateThumbnail(item, ByteArrayInputStream(ByteArray(0)))
            assertTrue(thumb != null && thumb.size == 2)
            assertEquals("image/jpeg", hook.getThumbnailMimeType())
            assertEquals(256, hook.getThumbnailMaxSize())
        }
    }

    @Nested
    inner class TransformationHookTests {

        @Test
        fun `TransformationHook implementation returns result`() = runTest {
            val hook = object : TransformationHook {
                override suspend fun transform(
                    item: StorageItem,
                    stream: InputStream,
                    targetFormat: String,
                ): TransformationResult = TransformationResult(
                    data = byteArrayOf(1, 2, 3),
                    mimeType = targetFormat,
                )
                override fun getSupportedSourceFormats(): Set<String> = setOf("image/png")
                override fun getSupportedTargetFormats(): Set<String> = setOf("image/jpeg")
            }
            val item = StorageItem(
                id = "i",
                name = "x.png",
                path = "/x.png",
                type = ItemType.FILE,
                ownerId = "u",
                size = 0,
                mimeType = "image/png",
                createdAt = kotlinx.datetime.Instant.DISTANT_PAST,
                updatedAt = kotlinx.datetime.Instant.DISTANT_PAST,
                parentId = null,
                storageKey = null,
            )
            val result = hook.transform(item, ByteArrayInputStream(ByteArray(0)), "image/jpeg")
            assertEquals("image/jpeg", result.mimeType)
            assertEquals(3, result.data.size)
            assertEquals(setOf("image/png"), hook.getSupportedSourceFormats())
            assertEquals(setOf("image/jpeg"), hook.getSupportedTargetFormats())
        }
    }
}
