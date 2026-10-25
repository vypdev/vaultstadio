/**
 * Implementation of DisablePluginUseCase.
 */

package com.vaultstadio.app.data.plugin.usecase

import com.vaultstadio.app.domain.plugin.PluginRepository
import com.vaultstadio.app.domain.plugin.usecase.DisablePluginUseCase

class DisablePluginUseCaseImpl(
    private val pluginRepository: PluginRepository,
) : DisablePluginUseCase {

    override suspend fun invoke(pluginId: String) = pluginRepository.disablePlugin(pluginId)
}
