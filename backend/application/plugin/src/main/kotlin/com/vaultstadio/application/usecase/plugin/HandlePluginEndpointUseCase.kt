/**
 * Handle Plugin Endpoint Use Case
 *
 * Application use case for delegating a request to a plugin-registered endpoint.
 */

package com.vaultstadio.application.usecase.plugin

import com.vaultstadio.plugins.api.PluginManager
import com.vaultstadio.plugins.context.EndpointRequest
import com.vaultstadio.plugins.context.EndpointResponse

interface HandlePluginEndpointUseCase {

    suspend operator fun invoke(
        pluginId: String,
        method: String,
        path: String,
        request: EndpointRequest,
    ): EndpointResponse?
}

class HandlePluginEndpointUseCaseImpl(
    private val pluginManager: PluginManager,
) : HandlePluginEndpointUseCase {

    override suspend fun invoke(
        pluginId: String,
        method: String,
        path: String,
        request: EndpointRequest,
    ): EndpointResponse? =
        pluginManager.handlePluginEndpoint(pluginId, method, path, request)
}
