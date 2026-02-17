/**
 * Plugin Service
 */

package com.vaultstadio.app.data.service

import com.vaultstadio.app.data.api.PluginApi
import com.vaultstadio.app.data.mapper.toDomain
import com.vaultstadio.app.data.mapper.toPluginList
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.model.PluginInfo
import org.koin.core.annotation.Single

@Single
class PluginService(private val pluginApi: PluginApi) {
    suspend fun getPlugins(): ApiResult<List<PluginInfo>> = pluginApi.getPlugins().map { it.toPluginList() }
    suspend fun enablePlugin(
        pluginId: String,
    ): ApiResult<PluginInfo> = pluginApi.enablePlugin(pluginId).map { it.toDomain() }
    suspend fun disablePlugin(pluginId: String): ApiResult<Unit> = pluginApi.disablePlugin(pluginId)
}
