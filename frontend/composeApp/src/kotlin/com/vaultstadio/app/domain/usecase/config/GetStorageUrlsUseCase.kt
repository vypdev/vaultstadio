/**
 * Get Storage URLs Use Case
 *
 * Provides URLs for storage-related operations (download, thumbnail, preview).
 */

package com.vaultstadio.app.domain.usecase.config

import com.vaultstadio.app.data.repository.ConfigRepository
/**
 * Use case for building storage-related URLs.
 *
 * Encapsulates URL construction so ViewModels don't need to know API structure.
 */
class GetStorageUrlsUseCase(
    private val configRepository: ConfigRepository,
) {
    /**
     * Returns the download URL for a storage item.
     */
    fun downloadUrl(itemId: String): String =
        "${configRepository.getApiBaseUrl()}/api/v1/storage/download/$itemId"

    /**
     * Returns the thumbnail URL for a storage item.
     */
    fun thumbnailUrl(itemId: String, size: String = "medium"): String =
        "${configRepository.getApiBaseUrl()}/api/v1/storage/item/$itemId/thumbnail?size=$size"

    /**
     * Returns the preview URL for a storage item.
     */
    fun previewUrl(itemId: String): String =
        "${configRepository.getApiBaseUrl()}/api/v1/storage/item/$itemId/preview"

    /**
     * Returns the URL for batch downloading items as a ZIP file.
     */
    fun batchDownloadZipUrl(): String =
        "${configRepository.getApiBaseUrl()}/api/v1/storage/batch/download-zip"
}
