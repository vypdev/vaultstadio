/**
 * Implementation of GetCollaborationUrlUseCase.
 */

package com.vaultstadio.app.data.config.usecase

import com.vaultstadio.app.domain.config.ConfigRepository
import com.vaultstadio.app.domain.config.usecase.GetCollaborationUrlUseCase

class GetCollaborationUrlUseCaseImpl(
    private val configRepository: ConfigRepository,
) : GetCollaborationUrlUseCase {
    override fun invoke(): String = configRepository.getApiBaseUrl()
}
