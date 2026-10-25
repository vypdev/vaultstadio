/**
 * VaultStadio Metadata Extractor API Tests
 *
 * Unit tests for MetadataKeys, ExtractedMetadata, MetadataExtractorConfig, MetadataExtractionHook,
 * and MetadataExtractorPlugin.
 */

package com.vaultstadio.plugins.metadata

import arrow.core.right
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.core.domain.event.FileEvent
import com.vaultstadio.core.domain.model.StorageItemMetadata
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.plugins.api.PluginMetadata
import com.vaultstadio.plugins.context.AIApi
import com.vaultstadio.plugins.context.ConfigStore
import com.vaultstadio.plugins.context.EndpointRequest
import com.vaultstadio.plugins.context.EndpointResponse
import com.vaultstadio.plugins.context.HttpClientApi
import com.vaultstadio.plugins.context.MetadataApi
import com.vaultstadio.plugins.context.PluginContext
import com.vaultstadio.plugins.context.PluginLogger
import com.vaultstadio.plugins.context.StorageApi
import com.vaultstadio.plugins.context.UserApi
import com.vaultstadio.plugins.hooks.MetadataExtractionHook
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.coroutines.EmptyCoroutineContext

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

    @Nested
    inner class MetadataExtractorPluginTests {

        @Test
        fun `concrete MetadataExtractorPlugin extractMetadata returns extract values`() = runTest {
            val plugin = TestMetadataExtractorPlugin()
            val item = StorageItem(
                id = "item-1",
                name = "test.jpg",
                path = "/test.jpg",
                type = ItemType.FILE,
                ownerId = "user-1",
                size = 100,
                mimeType = "image/jpeg",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
            )
            val stream = ByteArrayInputStream(ByteArray(0))
            val result = plugin.extractMetadata(item, stream)
            assertEquals(mapOf("key" to "value", "count" to "42"), result)
            assertEquals(setOf("image/jpeg", "image/png"), plugin.getSupportedMimeTypes())
        }

        @Test
        fun `onInitialize subscribes and handler extracts and saves metadata on FileEvent Uploaded`() = runTest {
            val eventBus = EventBus()
            val storage = mockk<StorageApi>()
            val metadataApi = mockk<MetadataApi>()
            val logger = mockk<PluginLogger>(relaxed = true)
            coEvery { storage.readFile(any()) } returns ByteArrayInputStream(ByteArray(0)).right()
            coEvery { metadataApi.setValues(any(), any()) } returns emptyList<StorageItemMetadata>().right()
            val context = object : PluginContext {
                override val pluginId: String get() = "test-extractor"
                override val scope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)
                override val eventBus: EventBus = eventBus
                override val storage: StorageApi = storage
                override val metadata: MetadataApi = metadataApi
                override val users: UserApi get() = mockk(relaxed = true)
                override val logger: PluginLogger = logger
                override val config: ConfigStore get() = mockk(relaxed = true)
                override val tempDirectory: Path get() = Paths.get("tmp")
                override val dataDirectory: Path get() = Paths.get("data")
                override val httpClient: HttpClientApi? = null
                override val ai: AIApi? = null
                override fun registerEndpoint(
                    method: String,
                    path: String,
                    handler: suspend (EndpointRequest) -> EndpointResponse,
                ) {}
                override fun unregisterEndpoint(method: String, path: String) {}
                override suspend fun scheduleTask(
                    name: String,
                    cronExpression: String?,
                    task: suspend () -> Unit,
                ): String = "task-id"
                override suspend fun cancelTask(taskId: String) {}
            }
            val plugin = TestMetadataExtractorPlugin()
            plugin.onInitialize(context)
            val item = StorageItem(
                id = "file-1",
                name = "photo.jpg",
                path = "/photo.jpg",
                type = ItemType.FILE,
                ownerId = "user-1",
                size = 50,
                mimeType = "image/jpeg",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
            )
            val event = FileEvent.Uploaded(
                id = "ev-1",
                timestamp = Clock.System.now(),
                userId = "user-1",
                item = item,
            )
            eventBus.publish(event, async = false)
            coVerify(exactly = 1) { metadataApi.setValues("file-1", mapOf("key" to "value", "count" to "42")) }
            eventBus.shutdown()
        }
    }

    private class TestMetadataExtractorPlugin : MetadataExtractorPlugin() {
        override val metadata = PluginMetadata(
            id = "test-metadata-plugin",
            name = "Test Metadata",
            version = "1.0",
            description = "Test",
            author = "Test",
        )
        override val extractorConfig = MetadataExtractorConfig()
        override fun getSupportedMimeTypes(): Set<String> = setOf("image/jpeg", "image/png")
        override suspend fun extract(item: StorageItem, inputStream: InputStream): ExtractedMetadata =
            ExtractedMetadata(values = mapOf("key" to "value", "count" to "42"))
    }
}
