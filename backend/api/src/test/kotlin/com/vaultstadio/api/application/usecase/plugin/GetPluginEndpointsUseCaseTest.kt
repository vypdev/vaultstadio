/**
 * GetPluginEndpointsUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.plugin

import com.vaultstadio.api.plugins.PluginManager
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetPluginEndpointsUseCaseTest {

    private val pluginManager: PluginManager = mockk()
    private val useCase = GetPluginEndpointsUseCaseImpl(pluginManager)

    @Test
    fun invokeDelegatesToPluginManagerAndReturnsEndpoints() {
        val endpoints = setOf("/api/plugin/test/foo", "/api/plugin/test/bar")
        every { pluginManager.getPluginEndpoints("test-plugin") } returns endpoints

        val result = useCase("test-plugin")

        assertEquals(2, result.size)
        assertTrue(result.contains("/api/plugin/test/foo"))
        assertTrue(result.contains("/api/plugin/test/bar"))
    }

    @Test
    fun invokeReturnsEmptySetWhenPluginHasNoEndpoints() {
        every { pluginManager.getPluginEndpoints("empty-plugin") } returns emptySet()

        val result = useCase("empty-plugin")

        assertTrue(result.isEmpty())
    }
}
