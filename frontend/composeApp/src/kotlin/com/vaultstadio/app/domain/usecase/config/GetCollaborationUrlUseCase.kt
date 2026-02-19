/**
 * Get Collaboration URL Use Case
 *
 * Provides base URL for collaboration WebSocket connections.
 */

package com.vaultstadio.app.domain.usecase.config

import com.vaultstadio.app.data.repository.ConfigRepository
/**
 * Use case for getting the base URL for collaboration features.
 */
class GetCollaborationUrlUseCase(
    private val configRepository: ConfigRepository,
) {
    /**
     * Returns the base URL for WebSocket connections.
     */
    operator fun invoke(): String = configRepository.getApiBaseUrl()
}
