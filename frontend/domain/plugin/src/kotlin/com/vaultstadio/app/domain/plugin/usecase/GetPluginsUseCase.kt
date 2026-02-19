/**
 * Use case for getting available plugins.
 */

package com.vaultstadio.app.domain.plugin.usecase

import com.vaultstadio.app.domain.plugin.model.PluginInfo
import com.vaultstadio.app.domain.result.Result

interface GetPluginsUseCase {
    suspend operator fun invoke(): Result<List<PluginInfo>>
}
