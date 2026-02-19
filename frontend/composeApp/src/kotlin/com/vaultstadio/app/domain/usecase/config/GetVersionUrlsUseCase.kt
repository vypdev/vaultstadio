/**
 * Get Version URLs Use Case
 *
 * Provides URLs for version history operations.
 */

package com.vaultstadio.app.domain.usecase.config

import com.vaultstadio.app.data.repository.ConfigRepository
/**
 * Use case for building version-related URLs.
 */
class GetVersionUrlsUseCase(
    private val configRepository: ConfigRepository,
) {
    /**
     * Returns the download URL for a specific version of an item.
     */
    fun downloadUrl(itemId: String, versionNumber: Int): String =
        "${configRepository.getApiBaseUrl()}/api/v1/versions/$itemId/download/$versionNumber"
}
