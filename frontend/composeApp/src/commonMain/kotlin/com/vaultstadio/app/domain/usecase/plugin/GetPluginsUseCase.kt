/**
 * Get Plugins Use Case
 */

package com.vaultstadio.app.domain.usecase.plugin

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.PluginRepository
import com.vaultstadio.app.domain.model.PluginInfo
import org.koin.core.annotation.Factory

/**
 * Use case for getting available plugins.
 */
interface GetPluginsUseCase {
    suspend operator fun invoke(): ApiResult<List<PluginInfo>>
}

@Factory(binds = [GetPluginsUseCase::class])
class GetPluginsUseCaseImpl(
    private val pluginRepository: PluginRepository,
) : GetPluginsUseCase {

    override suspend operator fun invoke(): ApiResult<List<PluginInfo>> =
        pluginRepository.getPlugins()
}
