/**
 * GetPluginUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.plugin

import com.vaultstadio.application.usecase.plugin.GetPluginUseCaseImpl
import com.vaultstadio.plugins.api.Plugin
import com.vaultstadio.plugins.api.PluginManager
import com.vaultstadio.plugins.api.PluginMetadata
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GetPluginUseCaseTest {

    private val pluginManager: PluginManager = mockk()
    private val useCase = GetPluginUseCaseImpl(pluginManager)

    @Test
    fun invokeReturnsPluginWhenFound() {
        val plugin: Plugin = mockk()
        every { plugin.metadata } returns PluginMetadata(
            id = "test-plugin",
            name = "Test Plugin",
            version = "1.0.0",
            description = "Test",
            author = "Author",
        )
        every { pluginManager.getPlugin("test-plugin") } returns plugin

        val result = useCase("test-plugin")

        assertEquals(plugin, result)
        assertEquals("test-plugin", result?.metadata?.id)
    }

    @Test
    fun invokeReturnsNullWhenNotFound() {
        every { pluginManager.getPlugin("missing") } returns null

        val result = useCase("missing")

        assertNull(result)
    }
}
