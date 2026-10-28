/**
 * Implementation of GetStorageUrlsUseCase.
 */

package com.vaultstadio.app.data.config.usecase

import com.vaultstadio.app.data.network.apiV1Location
import com.vaultstadio.app.domain.config.ConfigRepository
import com.vaultstadio.app.domain.config.usecase.GetStorageUrlsUseCase

class GetStorageUrlsUseCaseImpl(
    private val configRepository: ConfigRepository,
) : GetStorageUrlsUseCase {
    override fun downloadUrl(itemId: String): String =
        apiV1Location(configRepository.getApiBaseUrl(), listOf("storage", "download", itemId))

    override fun thumbnailUrl(itemId: String, size: String): String =
        apiV1Location(
            configRepository.getApiBaseUrl(),
            listOf("storage", "item", itemId, "thumbnail"),
            mapOf(
                "size" to size,
            ),
        )

    override fun previewUrl(itemId: String): String =
        apiV1Location(configRepository.getApiBaseUrl(), listOf("storage", "item", itemId, "preview"))

    override fun batchDownloadZipUrl(): String =
        apiV1Location(configRepository.getApiBaseUrl(), listOf("storage", "batch", "download-zip"))
}
