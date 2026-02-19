/**
 * GetPluginStateUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.plugin

import com.vaultstadio.api.plugins.PluginManager
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import com.vaultstadio.plugins.api.PluginState

class GetPluginStateUseCaseTest {

    private val pluginManager: PluginManager = mockk()
    private val useCase = GetPluginStateUseCaseImpl(pluginManager)

    @Test
    fun isPluginEnabledReturnsTrueWhenPluginManagerReturnsTrue() {
        every { pluginManager.isPluginEnabled("test-plugin") } returns true

        val result = useCase.isPluginEnabled("test-plugin")

        assertTrue(result)
    }

    @Test
    fun isPluginEnabledReturnsFalseWhenPluginManagerReturnsFalse() {
        every { pluginManager.isPluginEnabled("disabled-plugin") } returns false

        val result = useCase.isPluginEnabled("disabled-plugin")

        assertFalse(result)
    }

    @Test
    fun getPluginStateDelegatesToPluginManagerAndReturnsState() {
        every { pluginManager.getPluginState("test-plugin") } returns PluginState.RUNNING

        val result = useCase.getPluginState("test-plugin")

        assertEquals(PluginState.RUNNING, result)
    }

    @Test
    fun getPluginStateReturnsStoppedWhenPluginManagerReturnsStopped() {
        every { pluginManager.getPluginState("stopped-plugin") } returns PluginState.STOPPED

        val result = useCase.getPluginState("stopped-plugin")

        assertEquals(PluginState.STOPPED, result)
    }
}
