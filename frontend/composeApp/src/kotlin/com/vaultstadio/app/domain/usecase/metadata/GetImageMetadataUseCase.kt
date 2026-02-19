/**
 * Get Image Metadata Use Case
 */

package com.vaultstadio.app.domain.usecase.metadata

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.MetadataRepository
import com.vaultstadio.app.domain.model.ImageMetadata
import org.koin.core.annotation.Factory

/**
 * Use case for getting image metadata (EXIF, dimensions, etc.).
 */
interface GetImageMetadataUseCase {
    suspend operator fun invoke(itemId: String): Result<ImageMetadata>
}

@Factory(binds = [GetImageMetadataUseCase::class])
class GetImageMetadataUseCaseImpl(
    private val metadataRepository: MetadataRepository,
) : GetImageMetadataUseCase {

    override suspend operator fun invoke(itemId: String): Result<ImageMetadata> =
        metadataRepository.getImageMetadata(itemId)
}
