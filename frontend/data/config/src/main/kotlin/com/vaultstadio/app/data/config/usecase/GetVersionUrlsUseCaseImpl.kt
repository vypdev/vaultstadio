/**
 * Implementation of GetVersionUrlsUseCase.
 */

package com.vaultstadio.app.data.config.usecase

import com.vaultstadio.app.data.network.apiV1Location
import com.vaultstadio.app.domain.config.ConfigRepository
import com.vaultstadio.app.domain.config.usecase.GetVersionUrlsUseCase

class GetVersionUrlsUseCaseImpl(
    private val configRepository: ConfigRepository,
) : GetVersionUrlsUseCase {
    override fun downloadUrl(itemId: String, versionNumber: Int): String =
        apiV1Location(
            configRepository.getApiBaseUrl(),
            listOf("versions", "item", itemId, "version", versionNumber.toString(), "download"),
        )
}
