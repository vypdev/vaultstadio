/**
 * Plugin Repository implementation
 */

package com.vaultstadio.app.data.plugin.repository

import com.vaultstadio.app.data.network.mapper.toResult
import com.vaultstadio.app.data.plugin.service.PluginService
import com.vaultstadio.app.domain.plugin.PluginRepository

class PluginRepositoryImpl(
    private val pluginService: PluginService,
) : PluginRepository {

    override suspend fun getPlugins() = pluginService.getPlugins().toResult()
    override suspend fun enablePlugin(pluginId: String) = pluginService.enablePlugin(pluginId).toResult()
    override suspend fun disablePlugin(pluginId: String) = pluginService.disablePlugin(pluginId).toResult()
}
