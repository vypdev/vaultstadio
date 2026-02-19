/**
 * Plugin Repository
 */

package com.vaultstadio.app.data.repository

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.service.PluginService
import com.vaultstadio.app.domain.model.PluginInfo
import org.koin.core.annotation.Single

/**
 * Repository interface for plugin operations.
 */
interface PluginRepository {
    suspend fun getPlugins(): ApiResult<List<PluginInfo>>
    suspend fun enablePlugin(pluginId: String): ApiResult<PluginInfo>
    suspend fun disablePlugin(pluginId: String): ApiResult<Unit>
}

@Single(binds = [PluginRepository::class])
class PluginRepositoryImpl(
    private val pluginService: PluginService,
) : PluginRepository {

    override suspend fun getPlugins(): ApiResult<List<PluginInfo>> = pluginService.getPlugins()
    override suspend fun enablePlugin(pluginId: String): ApiResult<PluginInfo> = pluginService.enablePlugin(pluginId)
    override suspend fun disablePlugin(pluginId: String): ApiResult<Unit> = pluginService.disablePlugin(pluginId)
}
