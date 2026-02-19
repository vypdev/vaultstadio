/**
 * HandlePluginEndpointUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.plugin

import com.vaultstadio.api.plugins.PluginManager
import com.vaultstadio.plugins.context.EndpointRequest
import com.vaultstadio.plugins.context.EndpointResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HandlePluginEndpointUseCaseTest {

    private val pluginManager: PluginManager = mockk()
    private val useCase = HandlePluginEndpointUseCaseImpl(pluginManager)

    @Test
    fun invokeDelegatesToPluginManagerAndReturnsResponse() = runTest {
        val request = EndpointRequest(
            method = "GET",
            path = "/foo",
            headers = emptyMap(),
            queryParams = emptyMap(),
            body = null,
            userId = "user-1",
        )
        val response = EndpointResponse.ok("data")
        coEvery {
            pluginManager.handlePluginEndpoint("test-plugin", "GET", "/foo", request)
        } returns response

        val result = useCase("test-plugin", "GET", "/foo", request)

        assertNotNull(result)
        assertEquals(200, result!!.statusCode)
        assertEquals("data", result.body)
    }

    @Test
    fun invokeReturnsNullWhenPluginManagerReturnsNull() = runTest {
        val request = EndpointRequest(
            method = "GET",
            path = "/missing",
            headers = emptyMap(),
            queryParams = emptyMap(),
            body = null,
            userId = null,
        )
        coEvery {
            pluginManager.handlePluginEndpoint("test-plugin", "GET", "/missing", request)
        } returns null

        val result = useCase("test-plugin", "GET", "/missing", request)

        assertNull(result)
    }
}
