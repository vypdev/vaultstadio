/**
 * VaultStadio Plugin API Tests
 *
 * Unit tests for Plugin interface, PluginMetadata, PluginState, PluginHealthStatus, and AbstractPlugin.
 */

package com.vaultstadio.plugins.api

import com.vaultstadio.plugins.context.PluginContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.coroutines.EmptyCoroutineContext

class PluginTest {

    @Nested
    inner class PluginMetadataTests {

        @Test
        fun `metadata holds required fields`() {
            val meta = PluginMetadata(
                id = "com.test.plugin",
                name = "Test Plugin",
                version = "1.0.0",
                description = "A test",
                author = "Author",
            )
            assertEquals("com.test.plugin", meta.id)
            assertEquals("Test Plugin", meta.name)
            assertEquals("1.0.0", meta.version)
            assertEquals("A test", meta.description)
            assertEquals("Author", meta.author)
        }

        @Test
        fun `metadata defaults for optional fields`() {
            val meta = PluginMetadata(
                id = "id",
                name = "n",
                version = "1",
                description = "d",
                author = "a",
            )
            assertNull(meta.website)
            assertEquals("1.0.0", meta.minCoreVersion)
            assertTrue(meta.permissions.isEmpty())
            assertTrue(meta.dependencies.isEmpty())
            assertTrue(meta.tags.isEmpty())
            assertTrue(meta.supportedMimeTypes.isEmpty())
        }

        @Test
        fun `metadata with optional fields`() {
            val meta = PluginMetadata(
                id = "id",
                name = "n",
                version = "1",
                description = "d",
                author = "a",
                website = "https://example.com",
                permissions = setOf(PluginPermission.READ_FILES),
                dependencies = listOf(PluginDependency("other", "1.0.0")),
                tags = listOf("tag1"),
                supportedMimeTypes = setOf("image/*"),
            )
            assertEquals("https://example.com", meta.website)
            assertEquals(1, meta.permissions.size)
            assertTrue(meta.permissions.contains(PluginPermission.READ_FILES))
            assertEquals(1, meta.dependencies.size)
            assertEquals("other", meta.dependencies[0].pluginId)
            assertEquals("1.0.0", meta.dependencies[0].minVersion)
            assertFalse(meta.dependencies[0].optional)
            assertEquals(listOf("tag1"), meta.tags)
            assertEquals(setOf("image/*"), meta.supportedMimeTypes)
        }
    }

    @Nested
    inner class PluginPermissionTests {

        @Test
        fun `all permission enum values are distinct`() {
            val values = PluginPermission.entries
            assertEquals(values.size, values.map { it.name }.toSet().size)
        }

        @Test
        fun `common permissions exist`() {
            assertTrue(PluginPermission.entries.any { it == PluginPermission.READ_FILES })
            assertTrue(PluginPermission.entries.any { it == PluginPermission.WRITE_METADATA })
        }
    }

    @Nested
    inner class PluginDependencyTests {

        @Test
        fun `dependency holds pluginId and minVersion`() {
            val dep = PluginDependency(pluginId = "x", minVersion = "2.0")
            assertEquals("x", dep.pluginId)
            assertEquals("2.0", dep.minVersion)
            assertFalse(dep.optional)
        }

        @Test
        fun `dependency optional default is false`() {
            val dep = PluginDependency(pluginId = "x", minVersion = "1", optional = true)
            assertTrue(dep.optional)
        }
    }

    @Nested
    inner class PluginStateTests {

        @Test
        fun `all states are distinct`() {
            val states = PluginState.entries
            assertEquals(states.size, states.map { it.name }.toSet().size)
        }

        @Test
        fun `expected lifecycle states exist`() {
            assertTrue(PluginState.entries.contains(PluginState.REGISTERED))
            assertTrue(PluginState.entries.contains(PluginState.LOADED))
            assertTrue(PluginState.entries.contains(PluginState.RUNNING))
            assertTrue(PluginState.entries.contains(PluginState.STOPPED))
            assertTrue(PluginState.entries.contains(PluginState.FAILED))
        }
    }

    @Nested
    inner class PluginHealthStatusTests {

        @Test
        fun `Healthy is a valid status`() {
            val status = PluginHealthStatus.Healthy
            assertTrue(status is PluginHealthStatus.Healthy)
        }

        @Test
        fun `Degraded contains reason`() {
            val status = PluginHealthStatus.Degraded("slow")
            assertTrue(status is PluginHealthStatus.Degraded)
            assertEquals("slow", (status as PluginHealthStatus.Degraded).reason)
        }

        @Test
        fun `Unhealthy contains reason and optional error`() {
            val status = PluginHealthStatus.Unhealthy("failed", null)
            assertTrue(status is PluginHealthStatus.Unhealthy)
            assertEquals("failed", (status as PluginHealthStatus.Unhealthy).reason)
            assertNull((status).error)
        }
    }

    @Nested
    inner class PluginInterfaceDefaultTests {

        @Test
        fun `getConfigurationSchema default is null`() = runTest {
            val plugin = object : Plugin {
                override val metadata = PluginMetadata(
                    id = "x",
                    name = "x",
                    version = "1",
                    description = "d",
                    author = "a",
                )
                override suspend fun onInitialize(context: PluginContext) {}
            }
            assertNull(plugin.getConfigurationSchema())
        }

        @Test
        fun `getConfiguration default is empty map`() = runTest {
            val plugin = object : Plugin {
                override val metadata = PluginMetadata(
                    id = "x",
                    name = "x",
                    version = "1",
                    description = "d",
                    author = "a",
                )
                override suspend fun onInitialize(context: PluginContext) {}
            }
            assertTrue(plugin.getConfiguration().isEmpty())
        }

        @Test
        fun `updateConfiguration default returns true`() = runTest {
            val plugin = object : Plugin {
                override val metadata = PluginMetadata(
                    id = "x",
                    name = "x",
                    version = "1",
                    description = "d",
                    author = "a",
                )
                override suspend fun onInitialize(context: PluginContext) {}
            }
            assertTrue(plugin.updateConfiguration(emptyMap()))
        }

        @Test
        fun `healthCheck default is Healthy`() = runTest {
            val plugin = object : Plugin {
                override val metadata = PluginMetadata(
                    id = "x",
                    name = "x",
                    version = "1",
                    description = "d",
                    author = "a",
                )
                override suspend fun onInitialize(context: PluginContext) {}
            }
            assertTrue(plugin.healthCheck() is PluginHealthStatus.Healthy)
        }

        @Test
        fun `getStatistics default is empty map`() = runTest {
            val plugin = object : Plugin {
                override val metadata = PluginMetadata(
                    id = "x",
                    name = "x",
                    version = "1",
                    description = "d",
                    author = "a",
                )
                override suspend fun onInitialize(context: PluginContext) {}
            }
            assertTrue(plugin.getStatistics().isEmpty())
        }
    }

    @Nested
    inner class AbstractPluginTests {

        @Test
        fun `onLoad sets context and state to LOADED`() = runTest {
            val plugin = TestConcretePlugin()
            val context = mockPluginContext()
            plugin.onLoad(context)
            assertEquals(PluginState.LOADED, plugin.currentState())
            plugin.onShutdown()
        }

        @Test
        fun `onInitialize sets state to RUNNING`() = runTest {
            val plugin = TestConcretePlugin()
            val context = mockPluginContext()
            plugin.onLoad(context)
            plugin.onInitialize(context)
            assertEquals(PluginState.RUNNING, plugin.currentState())
            plugin.onShutdown()
        }

        @Test
        fun `onShutdown clears context and sets state to STOPPED`() = runTest {
            val plugin = TestConcretePlugin()
            val context = mockPluginContext()
            plugin.onLoad(context)
            plugin.onInitialize(context)
            plugin.onShutdown()
            assertEquals(PluginState.STOPPED, plugin.currentState())
            assertNull(plugin.contextForTest())
        }

        @Test
        fun `requireContext throws when not loaded`() {
            val plugin = TestConcretePlugin()
            assertThrows<IllegalStateException> {
                plugin.requireContextForTest()
            }
        }

        @Test
        fun `requireContext returns context when loaded`() = runTest {
            val plugin = TestConcretePlugin()
            val context = mockPluginContext()
            plugin.onLoad(context)
            assertEquals(context, plugin.requireContextForTest())
            plugin.onShutdown()
        }
    }

    private fun mockPluginContext(): PluginContext {
        return object : PluginContext {
            override val pluginId: String get() = "test"
            override val scope: CoroutineScope get() = CoroutineScope(EmptyCoroutineContext)
            override val eventBus get() = throw UnsupportedOperationException("mock")
            override val storage get() = throw UnsupportedOperationException("mock")
            override val metadata get() = throw UnsupportedOperationException("mock")
            override val users get() = throw UnsupportedOperationException("mock")
            override val logger get() = throw UnsupportedOperationException("mock")
            override val config get() = throw UnsupportedOperationException("mock")
            override val tempDirectory get() = throw UnsupportedOperationException("mock")
            override val dataDirectory get() = throw UnsupportedOperationException("mock")
            override val httpClient get() = null
            override val ai get() = null
            override fun registerEndpoint(
                method: String,
                path: String,
                handler: suspend (
                    com.vaultstadio.plugins.context.EndpointRequest,
                ) -> com.vaultstadio.plugins.context.EndpointResponse,
            ) {}
            override fun unregisterEndpoint(method: String, path: String) {}
            override suspend fun scheduleTask(
                name: String,
                cronExpression: String?,
                task: suspend () -> Unit,
            ): String = "task-1"
            override suspend fun cancelTask(taskId: String) {}
        }
    }

    private class TestConcretePlugin : AbstractPlugin() {
        override val metadata = PluginMetadata(
            id = "test-concrete",
            name = "Test",
            version = "1.0",
            description = "Test plugin",
            author = "Test",
        )
        override suspend fun onInitialize(context: PluginContext) {
            super.onInitialize(context)
        }
        fun currentState(): PluginState = state
        fun contextForTest(): PluginContext? = context
        fun requireContextForTest(): PluginContext = requireContext()
    }
}
