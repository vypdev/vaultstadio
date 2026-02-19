/**
 * Use case interface for building version-related URLs.
 */

package com.vaultstadio.app.domain.config.usecase

/**
 * Provides URLs for version history operations (e.g. download a specific version).
 */
interface GetVersionUrlsUseCase {
    fun downloadUrl(itemId: String, versionNumber: Int): String
}
