/**
 * Get Plugin Use Case
 *
 * Application use case for getting a plugin by ID with its runtime state.
 */

package com.vaultstadio.application.usecase.plugin

import com.vaultstadio.plugins.api.Plugin
import com.vaultstadio.plugins.api.PluginManager

interface GetPluginUseCase {

    operator fun invoke(pluginId: String): Plugin?
}

class GetPluginUseCaseImpl(
    private val pluginManager: PluginManager,
) : GetPluginUseCase {

    override fun invoke(pluginId: String): Plugin? = pluginManager.getPlugin(pluginId)
}
