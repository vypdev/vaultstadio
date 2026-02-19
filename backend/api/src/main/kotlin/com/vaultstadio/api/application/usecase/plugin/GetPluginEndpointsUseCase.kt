/**
 * Get Plugin Endpoints Use Case
 *
 * Application use case for listing endpoints registered by a plugin.
 */

package com.vaultstadio.api.application.usecase.plugin

import com.vaultstadio.api.plugins.PluginManager

interface GetPluginEndpointsUseCase {

    operator fun invoke(pluginId: String): Set<String>
}

class GetPluginEndpointsUseCaseImpl(
    private val pluginManager: PluginManager,
) : GetPluginEndpointsUseCase {

    override fun invoke(pluginId: String): Set<String> = pluginManager.getPluginEndpoints(pluginId)
}
