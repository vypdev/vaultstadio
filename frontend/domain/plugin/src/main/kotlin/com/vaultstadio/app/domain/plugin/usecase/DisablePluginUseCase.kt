/**
 * Use case for disabling a plugin.
 */

package com.vaultstadio.app.domain.plugin.usecase

import com.vaultstadio.app.domain.result.Result

interface DisablePluginUseCase {
    suspend operator fun invoke(pluginId: String): Result<Unit>
}
