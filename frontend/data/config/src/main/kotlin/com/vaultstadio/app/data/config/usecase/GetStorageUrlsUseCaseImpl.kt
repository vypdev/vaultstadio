/**
 * Implementation of GetStorageUrlsUseCase.
 */

package com.vaultstadio.app.data.config.usecase

import com.vaultstadio.app.domain.config.ConfigRepository
import com.vaultstadio.app.domain.config.usecase.GetStorageUrlsUseCase

class GetStorageUrlsUseCaseImpl(
    private val configRepository: ConfigRepository,
) : GetStorageUrlsUseCase {
    override fun downloadUrl(itemId: String): String =
        "${configRepository.getApiBaseUrl()}/api/v1/storage/download/$itemId"

    override fun thumbnailUrl(itemId: String, size: String): String =
        "${configRepository.getApiBaseUrl()}/api/v1/storage/item/$itemId/thumbnail?size=$size"

    override fun previewUrl(itemId: String): String =
        "${configRepository.getApiBaseUrl()}/api/v1/storage/item/$itemId/preview"

    override fun batchDownloadZipUrl(): String =
        "${configRepository.getApiBaseUrl()}/api/v1/storage/batch/download-zip"
}
