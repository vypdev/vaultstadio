/**
 * VaultStadio Plugin Manager Tests
 */

package com.vaultstadio.api.plugins

import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.core.domain.repository.MetadataRepository
import com.vaultstadio.core.domain.repository.StorageItemRepository
import com.vaultstadio.core.domain.repository.UserRepository
import com.vaultstadio.plugins.api.PluginState
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for PluginManager.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PluginManagerTest {

    private lateinit var pluginManager: PluginManager
    private lateinit var eventBus: EventBus
    private lateinit var storageItemRepository: StorageItemRepository
    private lateinit var metadataRepository: MetadataRepository
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setup() {
        eventBus = mockk(relaxed = true)
        storageItemRepository = mockk(relaxed = true)
        metadataRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)

        pluginManager = PluginManagerImpl(
            eventBus = eventBus,
            storageItemRepository = storageItemRepository,
            metadataRepository = metadataRepository,
            userRepository = userRepository,
        )
    }

    @Nested
    @DisplayName("Interface Implementation Tests")
    inner class InterfaceTests {

        @Test
        fun `pluginManager should implement PluginManager interface`() {
            assertTrue(pluginManager is PluginManager)
        }

        @Test
        fun `pluginManager should be of correct implementation type`() {
            assertTrue(pluginManager is PluginManagerImpl)
        }
    }

    @Nested
    @DisplayName("Plugin Listing Tests")
    inner class ListingTests {

        @Test
        fun `listPlugins should return empty list initially`() {
            val plugins = pluginManager.listPlugins()

            assertTrue(plugins.isEmpty())
        }

        @Test
        fun `getPlugin should return null for non-existent plugin`() {
            val plugin = pluginManager.getPlugin("non-existent-plugin")

            assertNull(plugin)
        }
    }

    @Nested
    @DisplayName("Plugin State Tests")
    inner class StateTests {

        @Test
        fun `isPluginEnabled should return false for non-existent plugin`() {
            val enabled = pluginManager.isPluginEnabled("non-existent-plugin")

            assertFalse(enabled)
        }

        @Test
        fun `getPluginState should return REGISTERED for non-existent plugin`() {
            val state = pluginManager.getPluginState("non-existent-plugin")

            assertEquals(PluginState.REGISTERED, state)
        }
    }

    @Nested
    @DisplayName("Plugin Enable/Disable Tests")
    inner class EnableDisableTests {

        @Test
        fun `enablePlugin should return error for non-existent plugin`() = runTest {
            val result = pluginManager.enablePlugin("non-existent-plugin")

            assertTrue(result.isLeft())
        }

        @Test
        fun `disablePlugin should return error for non-existent plugin`() = runTest {
            val result = pluginManager.disablePlugin("non-existent-plugin")

            assertTrue(result.isLeft())
        }
    }

    @Nested
    @DisplayName("Plugin Loading Tests")
    inner class LoadingTests {

        @Test
        fun `loadPlugins should complete without error`() = runTest {
            // This will use ServiceLoader to find plugins
            // In test environment, may find no plugins
            pluginManager.loadPlugins()

            // Should not throw
            assertNotNull(pluginManager)
        }
    }

    @Nested
    @DisplayName("Plugin Shutdown Tests")
    inner class ShutdownTests {

        @Test
        fun `shutdown should complete without error`() = runTest {
            pluginManager.shutdown()

            // Should not throw
            assertNotNull(pluginManager)
        }

        @Test
        fun `shutdown should be safe to call multiple times`() = runTest {
            pluginManager.shutdown()
            pluginManager.shutdown()

            // Should not throw
            assertNotNull(pluginManager)
        }
    }

    @Nested
    @DisplayName("PluginState Enum Tests")
    inner class PluginStateEnumTests {

        @Test
        fun `all plugin states should be defined`() {
            val states = PluginState.entries.toTypedArray()

            assertTrue(states.isNotEmpty())
        }

        @Test
        fun `REGISTERED state should exist`() {
            assertEquals("REGISTERED", PluginState.REGISTERED.name)
        }

        @Test
        fun `LOADING state should exist`() {
            assertEquals("LOADING", PluginState.LOADING.name)
        }

        @Test
        fun `LOADED state should exist`() {
            assertEquals("LOADED", PluginState.LOADED.name)
        }

        @Test
        fun `INITIALIZING state should exist`() {
            assertEquals("INITIALIZING", PluginState.INITIALIZING.name)
        }

        @Test
        fun `RUNNING state should exist`() {
            assertEquals("RUNNING", PluginState.RUNNING.name)
        }

        @Test
        fun `STOPPING state should exist`() {
            assertEquals("STOPPING", PluginState.STOPPING.name)
        }

        @Test
        fun `STOPPED state should exist`() {
            assertEquals("STOPPED", PluginState.STOPPED.name)
        }

        @Test
        fun `DISABLED state should exist`() {
            assertEquals("DISABLED", PluginState.DISABLED.name)
        }

        @Test
        fun `FAILED state should exist`() {
            assertEquals("FAILED", PluginState.FAILED.name)
        }
    }

    @Nested
    @DisplayName("Method Signature Tests")
    inner class MethodSignatureTests {

        @Test
        fun `listPlugins method should exist`() {
            assertNotNull(pluginManager::listPlugins)
        }

        @Test
        fun `getPlugin method should exist`() {
            assertNotNull(pluginManager::getPlugin)
        }

        @Test
        fun `isPluginEnabled method should exist`() {
            assertNotNull(pluginManager::isPluginEnabled)
        }

        @Test
        fun `getPluginState method should exist`() {
            assertNotNull(pluginManager::getPluginState)
        }

        @Test
        fun `enablePlugin method should exist`() {
            assertNotNull(pluginManager::enablePlugin)
        }

        @Test
        fun `disablePlugin method should exist`() {
            assertNotNull(pluginManager::disablePlugin)
        }

        @Test
        fun `loadPlugins method should exist`() {
            assertNotNull(pluginManager::loadPlugins)
        }

        @Test
        fun `shutdown method should exist`() {
            assertNotNull(pluginManager::shutdown)
        }
    }
}
