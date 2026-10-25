/**
 * Plugin manager port.
 *
 * Application use cases depend on this interface; the API layer provides the implementation.
 */

package com.vaultstadio.plugins.api

import arrow.core.Either
import com.vaultstadio.domain.common.exception.PluginException
import com.vaultstadio.plugins.context.EndpointRequest
import com.vaultstadio.plugins.context.EndpointResponse

/**
 * Port for plugin lifecycle and endpoint handling.
 */
interface PluginManager {

    fun listPlugins(): List<Plugin>
    fun getPlugin(pluginId: String): Plugin?
    fun isPluginEnabled(pluginId: String): Boolean
    fun getPluginState(pluginId: String): PluginState
    suspend fun enablePlugin(pluginId: String): Either<PluginException, Unit>
    suspend fun disablePlugin(pluginId: String): Either<PluginException, Unit>
    suspend fun loadPlugins()
    suspend fun shutdown()

    suspend fun handlePluginEndpoint(
        pluginId: String,
        method: String,
        path: String,
        request: EndpointRequest,
    ): EndpointResponse?

    fun getPluginEndpoints(pluginId: String): Set<String>
}
