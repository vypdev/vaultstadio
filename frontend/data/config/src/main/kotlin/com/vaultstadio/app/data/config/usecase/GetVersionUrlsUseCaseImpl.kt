/**
 * Implementation of GetVersionUrlsUseCase.
 */

package com.vaultstadio.app.data.config.usecase

import com.vaultstadio.app.domain.config.ConfigRepository
import com.vaultstadio.app.domain.config.usecase.GetVersionUrlsUseCase

class GetVersionUrlsUseCaseImpl(
    private val configRepository: ConfigRepository,
) : GetVersionUrlsUseCase {
    override fun downloadUrl(itemId: String, versionNumber: Int): String =
        "${configRepository.getApiBaseUrl()}/api/v1/versions/$itemId/download/$versionNumber"
}
