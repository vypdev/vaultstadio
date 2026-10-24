/**
 * Implementation of EnablePluginUseCase.
 */

package com.vaultstadio.app.data.plugin.usecase

import com.vaultstadio.app.domain.plugin.PluginRepository
import com.vaultstadio.app.domain.plugin.usecase.EnablePluginUseCase

class EnablePluginUseCaseImpl(
    private val pluginRepository: PluginRepository,
) : EnablePluginUseCase {

    override suspend fun invoke(pluginId: String) = pluginRepository.enablePlugin(pluginId)
}
