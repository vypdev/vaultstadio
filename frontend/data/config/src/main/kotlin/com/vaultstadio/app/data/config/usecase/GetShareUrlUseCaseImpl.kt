/**
 * Implementation of GetShareUrlUseCase.
 */

package com.vaultstadio.app.data.config.usecase

import com.vaultstadio.app.data.network.apiV1Location
import com.vaultstadio.app.domain.config.ConfigRepository
import com.vaultstadio.app.domain.config.usecase.GetShareUrlUseCase

class GetShareUrlUseCaseImpl(
    private val configRepository: ConfigRepository,
) : GetShareUrlUseCase {
    override fun invoke(token: String): String =
        apiV1Location(configRepository.getApiBaseUrl(), listOf("share", token, "view"))
}
