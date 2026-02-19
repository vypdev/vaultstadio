/**
 * Repository interface for plugin operations.
 */

package com.vaultstadio.app.domain.plugin

import com.vaultstadio.app.domain.plugin.model.PluginInfo
import com.vaultstadio.app.domain.result.Result

interface PluginRepository {
    suspend fun getPlugins(): Result<List<PluginInfo>>
    suspend fun enablePlugin(pluginId: String): Result<PluginInfo>
    suspend fun disablePlugin(pluginId: String): Result<Unit>
}
