/**
 * Use case interface for building storage-related URLs.
 */

package com.vaultstadio.app.domain.config.usecase

/**
 * Encapsulates URL construction for storage operations (download, thumbnail, preview).
 */
interface GetStorageUrlsUseCase {
    fun downloadUrl(itemId: String): String
    fun thumbnailUrl(itemId: String, size: String = "medium"): String
    fun previewUrl(itemId: String): String
    fun batchDownloadZipUrl(): String
}
