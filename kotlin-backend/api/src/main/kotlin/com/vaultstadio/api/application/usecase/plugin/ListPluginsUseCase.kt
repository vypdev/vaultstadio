/**
 * List Plugins Use Case
 *
 * Application use case for listing installed plugins with their state.
 */

package com.vaultstadio.api.application.usecase.plugin

import com.vaultstadio.api.plugins.PluginManager

interface ListPluginsUseCase {

    operator fun invoke(): List<PluginListEntry>
}

class ListPluginsUseCaseImpl(
    private val pluginManager: PluginManager,
) : ListPluginsUseCase {

    override fun invoke(): List<PluginListEntry> =
        pluginManager.listPlugins().map { plugin ->
            PluginListEntry(
                plugin = plugin,
                isEnabled = pluginManager.isPluginEnabled(plugin.metadata.id),
                state = pluginManager.getPluginState(plugin.metadata.id),
            )
        }
}
