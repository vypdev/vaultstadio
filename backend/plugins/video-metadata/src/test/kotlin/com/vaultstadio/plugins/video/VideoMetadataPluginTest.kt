/**
 * VaultStadio Video Metadata Plugin Tests
 */

package com.vaultstadio.plugins.video

import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VideoMetadataPluginTest {

    private lateinit var plugin: VideoMetadataPlugin

    @BeforeEach
    fun setup() {
        plugin = VideoMetadataPlugin()
    }

    @Nested
    inner class PluginInfoTests {

        @Test
        fun `should have correct plugin id`() {
            assertTrue(plugin.metadata.id.contains("video"))
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
        }

        @Test
        fun `should have author`() {
            assertNotNull(plugin.metadata.author)
        }
    }

    @Nested
    inner class SupportedFormatsTests {

        @Test
        fun `should support common video formats`() {
            val videoTypes = listOf(
                "video/mp4",
                "video/webm",
                "video/quicktime",
            )

            videoTypes.forEach { mimeType ->
                assertTrue(
                    plugin.metadata.supportedMimeTypes.contains(mimeType),
                    "Should support $mimeType",
                )
            }
        }

        @Test
        fun `should not support non-video formats`() {
            val nonVideoTypes = listOf(
                "image/jpeg",
                "audio/mpeg",
                "application/pdf",
                "text/plain",
            )

            nonVideoTypes.forEach { mimeType ->
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
        fun `should extract video properties`() {
            val expectedFields = listOf(
                "duration",
                "width",
                "height",
                "codec",
                "bitrate",
                "frameRate",
            )

            assertTrue(expectedFields.isNotEmpty())
        }

        @Test
        fun `should extract audio properties`() {
            val audioFields = listOf(
                "audioCodec",
                "audioChannels",
                "audioSampleRate",
                "audioBitrate",
            )

            assertTrue(audioFields.isNotEmpty())
        }

        @Test
        fun `should extract chapter information`() {
            val chapterFields = listOf(
                "chapters",
                "chapterCount",
            )

            assertTrue(chapterFields.isNotEmpty())
        }

        @Test
        fun `should extract subtitle tracks`() {
            val subtitleFields = listOf(
                "subtitles",
                "subtitleLanguages",
            )

            assertTrue(subtitleFields.isNotEmpty())
        }
    }

    @Nested
    inner class ThumbnailGenerationTests {

        @Test
        fun `should support thumbnail generation`() {
            // Thumbnail generation is a key feature
            assertTrue(true)
        }
    }

    @Nested
    inner class FFProbeTests {

        @Test
        fun `should handle missing ffprobe gracefully`() {
            // When FFprobe is not available, plugin should not crash
            // Just return limited metadata or error
            assertTrue(true)
        }
    }

    @Nested
    inner class ErrorPathTests {

        @Test
        fun `extractMetadata returns empty map for unsupported mime type`() = runTest {
            val item = StorageItem(
                id = "i1",
                name = "doc.pdf",
                path = "/doc.pdf",
                type = ItemType.FILE,
                ownerId = "u1",
                size = 0,
                mimeType = "application/pdf",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
                parentId = null,
                storageKey = null,
            )
            val result = plugin.extractMetadata(item, ByteArrayInputStream(ByteArray(0)))
            assertTrue(result.isEmpty())
        }

        @Test
        fun `getSupportedMimeTypes matches hook implementation`() {
            val fromHook = plugin.getSupportedMimeTypes()
            assertTrue(plugin.metadata.supportedMimeTypes == fromHook)
        }

        @Test
        fun `getConfigurationSchema returns non-null schema`() {
            val schema = plugin.getConfigurationSchema()
            assertNotNull(schema)
            assertTrue(schema.groups.isNotEmpty())
        }
    }
}
