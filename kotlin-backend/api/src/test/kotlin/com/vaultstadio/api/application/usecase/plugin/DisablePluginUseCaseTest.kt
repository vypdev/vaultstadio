/**
 * DisablePluginUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.plugin

import arrow.core.Either
import com.vaultstadio.api.plugins.PluginManager
import com.vaultstadio.core.exception.PluginNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DisablePluginUseCaseTest {

    private val pluginManager: PluginManager = mockk()
    private val useCase = DisablePluginUseCaseImpl(pluginManager)

    @Test
    fun invokeDelegatesToPluginManagerAndReturnsRightUnit() = runTest {
        coEvery { pluginManager.disablePlugin("test-plugin") } returns Either.Right(Unit)

        val result = useCase("test-plugin")

        assertTrue(result.isRight())
    }

    @Test
    fun invokeReturnsLeftWhenPluginManagerReturnsLeft() = runTest {
        coEvery { pluginManager.disablePlugin(any()) } returns
            Either.Left(PluginNotFoundException(pluginId = "test-plugin"))

        val result = useCase("test-plugin")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is PluginNotFoundException)
    }
}
