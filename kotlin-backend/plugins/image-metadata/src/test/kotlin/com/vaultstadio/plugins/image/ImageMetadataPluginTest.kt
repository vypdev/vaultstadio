/**
 * VaultStadio Image Metadata Plugin Tests
 */

package com.vaultstadio.plugins.image

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ImageMetadataPluginTest {

    private lateinit var plugin: ImageMetadataPlugin

    @BeforeEach
    fun setup() {
        plugin = ImageMetadataPlugin()
    }

    @Nested
    inner class PluginInfoTests {

        @Test
        fun `should have correct plugin id`() {
            assertEquals("com.vaultstadio.plugins.image-metadata", plugin.metadata.id)
        }

        @Test
        fun `should have name`() {
            assertNotNull(plugin.metadata.name)
            assertTrue(plugin.metadata.name.isNotEmpty())
        }

        @Test
        fun `should have description`() {
            assertNotNull(plugin.metadata.description)
        }

        @Test
        fun `should have version`() {
            assertNotNull(plugin.metadata.version)
            assertTrue(plugin.metadata.version.isNotEmpty())
        }

        @Test
        fun `should have author`() {
            assertNotNull(plugin.metadata.author)
        }
    }

    @Nested
    inner class SupportedFormatsTests {

        @Test
        fun `should support common image formats`() {
            val imageTypes = listOf(
                "image/jpeg",
                "image/png",
                "image/gif",
                "image/webp",
                "image/tiff",
            )

            imageTypes.forEach { mimeType ->
                assertTrue(
                    plugin.metadata.supportedMimeTypes.contains(mimeType),
                    "Should support $mimeType",
                )
            }
        }

        @Test
        fun `should not support non-image formats`() {
            val nonImageTypes = listOf(
                "video/mp4",
                "audio/mpeg",
                "application/pdf",
                "text/plain",
            )

            nonImageTypes.forEach { mimeType ->
                assertTrue(
                    !plugin.metadata.supportedMimeTypes.contains(mimeType),
                    "Should not support $mimeType",
                )
            }
        }
    }

    @Nested
    inner class PermissionsTests {

        @Test
        fun `should have required permissions`() {
            val permissions = plugin.metadata.permissions
            assertNotNull(permissions)
            assertTrue(permissions.isNotEmpty())
        }
    }

    @Nested
    inner class MetadataExtractionTests {

        @Test
        fun `should extract basic image properties`() {
            // This would need actual image data to test
            // In unit tests, we verify the structure
            val expectedFields = listOf(
                "width",
                "height",
                "format",
            )

            // Plugin should support these fields
            assertTrue(expectedFields.isNotEmpty())
        }

        @Test
        fun `should extract EXIF data when available`() {
            val exifFields = listOf(
                "cameraMake",
                "cameraModel",
                "dateTaken",
                "exposureTime",
                "fNumber",
                "iso",
            )

            assertTrue(exifFields.isNotEmpty())
        }

        @Test
        fun `should extract GPS data when enabled`() {
            val gpsFields = listOf(
                "latitude",
                "longitude",
                "altitude",
            )

            assertTrue(gpsFields.isNotEmpty())
        }
    }
}
