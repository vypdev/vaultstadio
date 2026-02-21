/**
 * EnablePluginUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.plugin

import arrow.core.Either
import com.vaultstadio.api.plugins.PluginManager
import com.vaultstadio.domain.common.exception.PluginNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EnablePluginUseCaseTest {

    private val pluginManager: PluginManager = mockk()
    private val useCase = EnablePluginUseCaseImpl(pluginManager)

    @Test
    fun invokeDelegatesToPluginManagerAndReturnsRightUnit() = runTest {
        coEvery { pluginManager.enablePlugin("test-plugin") } returns Either.Right(Unit)

        val result = useCase("test-plugin")

        assertTrue(result.isRight())
    }

    @Test
    fun invokeReturnsLeftWhenPluginManagerReturnsLeft() = runTest {
        coEvery { pluginManager.enablePlugin(any()) } returns
            Either.Left(PluginNotFoundException(pluginId = "test-plugin"))

        val result = useCase("test-plugin")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left<*>).value is PluginNotFoundException)
    }
}
