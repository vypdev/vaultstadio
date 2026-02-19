/**
 * Get Plugins Use Case
 */

package com.vaultstadio.app.domain.usecase.plugin

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.PluginRepository
import com.vaultstadio.app.domain.model.PluginInfo
/**
 * Use case for getting available plugins.
 */
interface GetPluginsUseCase {
    suspend operator fun invoke(): Result<List<PluginInfo>>
}

class GetPluginsUseCaseImpl(
    private val pluginRepository: PluginRepository,
) : GetPluginsUseCase {

    override suspend operator fun invoke(): Result<List<PluginInfo>> =
        pluginRepository.getPlugins()
}
