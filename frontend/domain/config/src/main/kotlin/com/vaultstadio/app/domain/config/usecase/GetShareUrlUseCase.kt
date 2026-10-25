/**
 * Use case interface for building share-related URLs.
 */

package com.vaultstadio.app.domain.config.usecase

/**
 * Provides the public URL for a share link token.
 */
interface GetShareUrlUseCase {
    operator fun invoke(token: String): String
}
