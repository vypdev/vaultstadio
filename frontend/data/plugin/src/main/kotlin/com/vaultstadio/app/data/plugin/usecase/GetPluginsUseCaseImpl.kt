/**
 * Implementation of GetPluginsUseCase.
 */

package com.vaultstadio.app.data.plugin.usecase

import com.vaultstadio.app.domain.plugin.PluginRepository
import com.vaultstadio.app.domain.plugin.usecase.GetPluginsUseCase

class GetPluginsUseCaseImpl(
    private val pluginRepository: PluginRepository,
) : GetPluginsUseCase {

    override suspend fun invoke() = pluginRepository.getPlugins()
}
