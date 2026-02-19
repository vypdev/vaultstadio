/**
 * Get Share URL Use Case
 *
 * Provides URLs for share links.
 */

package com.vaultstadio.app.domain.usecase.config

import com.vaultstadio.app.data.repository.ConfigRepository
import org.koin.core.annotation.Factory

/**
 * Use case for building share-related URLs.
 */
@Factory
class GetShareUrlUseCase(
    private val configRepository: ConfigRepository,
) {
    /**
     * Returns the public URL for a share link token.
     */
    operator fun invoke(token: String): String =
        "${configRepository.getApiBaseUrl()}/share/$token"
}
