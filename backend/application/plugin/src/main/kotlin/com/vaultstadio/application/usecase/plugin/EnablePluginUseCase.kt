/**
 * Enable Plugin Use Case
 *
 * Application use case for enabling a plugin.
 */

package com.vaultstadio.application.usecase.plugin

import arrow.core.Either
import com.vaultstadio.domain.common.exception.PluginException
import com.vaultstadio.plugins.api.PluginManager

interface EnablePluginUseCase {

    suspend operator fun invoke(pluginId: String): Either<PluginException, Unit>
}

class EnablePluginUseCaseImpl(
    private val pluginManager: PluginManager,
) : EnablePluginUseCase {

    override suspend fun invoke(pluginId: String): Either<PluginException, Unit> =
        pluginManager.enablePlugin(pluginId)
}
