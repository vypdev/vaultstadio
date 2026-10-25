/**
 * ListPluginsUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.plugin

import com.vaultstadio.application.usecase.plugin.ListPluginsUseCaseImpl
import com.vaultstadio.plugins.api.Plugin
import com.vaultstadio.plugins.api.PluginManager
import com.vaultstadio.plugins.api.PluginMetadata
import com.vaultstadio.plugins.api.PluginState
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ListPluginsUseCaseTest {

    private val pluginManager: PluginManager = mockk()
    private val useCase = ListPluginsUseCaseImpl(pluginManager)

    @Test
    fun invokeReturnsEntriesWithEnabledAndState() {
        val plugin: Plugin = mockk()
        every { plugin.metadata } returns PluginMetadata(
            id = "test-plugin",
            name = "Test Plugin",
            version = "1.0.0",
            description = "Test",
            author = "Test Author",
        )
        every { pluginManager.listPlugins() } returns listOf(plugin)
        every { pluginManager.isPluginEnabled("test-plugin") } returns true
        every { pluginManager.getPluginState("test-plugin") } returns PluginState.RUNNING

        val result = useCase()

        assertEquals(1, result.size)
        assertEquals(plugin, result[0].plugin)
        assertTrue(result[0].isEnabled)
        assertEquals(PluginState.RUNNING, result[0].state)
    }

    @Test
    fun invokeReturnsEmptyListWhenNoPlugins() {
        every { pluginManager.listPlugins() } returns emptyList()

        val result = useCase()

        assertTrue(result.isEmpty())
    }

    @Test
    fun invokeMapsDisabledPluginCorrectly() {
        val plugin: Plugin = mockk()
        every { plugin.metadata } returns PluginMetadata(
            id = "disabled-plugin",
            name = "Disabled",
            version = "1.0.0",
            description = "D",
            author = "A",
        )
        every { pluginManager.listPlugins() } returns listOf(plugin)
        every { pluginManager.isPluginEnabled("disabled-plugin") } returns false
        every { pluginManager.getPluginState("disabled-plugin") } returns PluginState.STOPPED

        val result = useCase()

        assertEquals(1, result.size)
        assertFalse(result[0].isEnabled)
        assertEquals(PluginState.STOPPED, result[0].state)
    }
}
