/**
 * Use case for enabling a plugin.
 */

package com.vaultstadio.app.domain.plugin.usecase

import com.vaultstadio.app.domain.plugin.model.PluginInfo
import com.vaultstadio.app.domain.result.Result

interface EnablePluginUseCase {
    suspend operator fun invoke(pluginId: String): Result<PluginInfo>
}
