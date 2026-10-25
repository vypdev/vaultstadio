/**
 * Get Plugin State Use Case
 *
 * Application use case for getting enabled state and state of a plugin.
 */

package com.vaultstadio.application.usecase.plugin

import com.vaultstadio.plugins.api.PluginManager
import com.vaultstadio.plugins.api.PluginState

interface GetPluginStateUseCase {

    fun isPluginEnabled(pluginId: String): Boolean

    fun getPluginState(pluginId: String): PluginState
}

class GetPluginStateUseCaseImpl(
    private val pluginManager: PluginManager,
) : GetPluginStateUseCase {

    override fun isPluginEnabled(pluginId: String): Boolean = pluginManager.isPluginEnabled(pluginId)

    override fun getPluginState(pluginId: String): PluginState = pluginManager.getPluginState(pluginId)
}
