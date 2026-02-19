/**
 * Enable Plugin Use Case
 */

package com.vaultstadio.app.domain.usecase.plugin

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.PluginRepository
import com.vaultstadio.app.domain.model.PluginInfo
import org.koin.core.annotation.Factory

/**
 * Use case for enabling a plugin.
 */
interface EnablePluginUseCase {
    suspend operator fun invoke(pluginId: String): Result<PluginInfo>
}

@Factory(binds = [EnablePluginUseCase::class])
class EnablePluginUseCaseImpl(
    private val pluginRepository: PluginRepository,
) : EnablePluginUseCase {

    override suspend operator fun invoke(pluginId: String): Result<PluginInfo> =
        pluginRepository.enablePlugin(pluginId)
}
