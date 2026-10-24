/**
 * Get image metadata use case implementation.
 */

package com.vaultstadio.app.data.metadata.usecase

import com.vaultstadio.app.domain.metadata.MetadataRepository
import com.vaultstadio.app.domain.metadata.model.ImageMetadata
import com.vaultstadio.app.domain.metadata.usecase.GetImageMetadataUseCase
import com.vaultstadio.app.domain.result.Result

class GetImageMetadataUseCaseImpl(
    private val metadataRepository: MetadataRepository,
) : GetImageMetadataUseCase {

    override suspend operator fun invoke(itemId: String): Result<ImageMetadata> =
        metadataRepository.getImageMetadata(itemId)
}
