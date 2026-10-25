/**
 * Implementation of GetShareUrlUseCase.
 */

package com.vaultstadio.app.data.config.usecase

import com.vaultstadio.app.domain.config.ConfigRepository
import com.vaultstadio.app.domain.config.usecase.GetShareUrlUseCase

class GetShareUrlUseCaseImpl(
    private val configRepository: ConfigRepository,
) : GetShareUrlUseCase {
    override fun invoke(token: String): String =
        "${configRepository.getApiBaseUrl()}/share/$token"
}
