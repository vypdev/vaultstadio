/**
 * Get video metadata use case implementation.
 */

package com.vaultstadio.app.data.metadata.usecase

import com.vaultstadio.app.domain.metadata.MetadataRepository
import com.vaultstadio.app.domain.metadata.model.VideoMetadata
import com.vaultstadio.app.domain.metadata.usecase.GetVideoMetadataUseCase
import com.vaultstadio.app.domain.result.Result

class GetVideoMetadataUseCaseImpl(
    private val metadataRepository: MetadataRepository,
) : GetVideoMetadataUseCase {

    override suspend operator fun invoke(itemId: String): Result<VideoMetadata> =
        metadataRepository.getVideoMetadata(itemId)
}
