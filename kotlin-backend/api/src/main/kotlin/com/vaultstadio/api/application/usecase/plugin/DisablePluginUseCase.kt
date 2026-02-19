/**
 * Disable Plugin Use Case
 *
 * Application use case for disabling a plugin.
 */

package com.vaultstadio.api.application.usecase.plugin

import arrow.core.Either
import com.vaultstadio.api.plugins.PluginManager
import com.vaultstadio.core.exception.PluginException

interface DisablePluginUseCase {

    suspend operator fun invoke(pluginId: String): Either<PluginException, Unit>
}

class DisablePluginUseCaseImpl(
    private val pluginManager: PluginManager,
) : DisablePluginUseCase {

    override suspend fun invoke(pluginId: String): Either<PluginException, Unit> =
        pluginManager.disablePlugin(pluginId)
}
