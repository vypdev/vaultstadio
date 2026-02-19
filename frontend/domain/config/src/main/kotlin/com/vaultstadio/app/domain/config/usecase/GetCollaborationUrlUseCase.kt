/**
 * Use case interface for collaboration WebSocket base URL.
 */

package com.vaultstadio.app.domain.config.usecase

/**
 * Provides the base URL for collaboration WebSocket connections.
 */
interface GetCollaborationUrlUseCase {
    operator fun invoke(): String
}
