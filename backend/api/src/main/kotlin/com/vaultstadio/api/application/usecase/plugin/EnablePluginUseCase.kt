/**
 * Enable Plugin Use Case
 *
 * Application use case for enabling a plugin.
 */

package com.vaultstadio.api.application.usecase.plugin

import arrow.core.Either
import com.vaultstadio.api.plugins.PluginManager
import com.vaultstadio.core.exception.PluginException

interface EnablePluginUseCase {

    suspend operator fun invoke(pluginId: String): Either<PluginException, Unit>
}

class EnablePluginUseCaseImpl(
    private val pluginManager: PluginManager,
) : EnablePluginUseCase {

    override suspend fun invoke(pluginId: String): Either<PluginException, Unit> =
        pluginManager.enablePlugin(pluginId)
}
