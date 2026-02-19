/**
 * Disable Plugin Use Case
 */

package com.vaultstadio.app.domain.usecase.plugin

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.PluginRepository
import org.koin.core.annotation.Factory

/**
 * Use case for disabling a plugin.
 */
interface DisablePluginUseCase {
    suspend operator fun invoke(pluginId: String): Result<Unit>
}

@Factory(binds = [DisablePluginUseCase::class])
class DisablePluginUseCaseImpl(
    private val pluginRepository: PluginRepository,
) : DisablePluginUseCase {

    override suspend operator fun invoke(pluginId: String): Result<Unit> =
        pluginRepository.disablePlugin(pluginId)
}
