/**
 * Plugin API
 */

package com.vaultstadio.app.data.api

import com.vaultstadio.app.data.dto.plugin.PluginInfoDTO
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.network.BaseApi
import io.ktor.client.HttpClient
import org.koin.core.annotation.Single

@Single
class PluginApi(client: HttpClient) : BaseApi(client) {
    suspend fun getPlugins(): ApiResult<List<PluginInfoDTO>> = get("/api/v1/plugins")
    suspend fun enablePlugin(
        pluginId: String,
    ): ApiResult<PluginInfoDTO> = postNoBody("/api/v1/plugins/$pluginId/enable")
    suspend fun disablePlugin(pluginId: String): ApiResult<Unit> = postNoBody("/api/v1/plugins/$pluginId/disable")
}
