/**
 * Get Video Metadata Use Case
 */

package com.vaultstadio.app.domain.usecase.metadata

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.MetadataRepository
import com.vaultstadio.app.domain.model.VideoMetadata
import org.koin.core.annotation.Factory

/**
 * Use case for getting video metadata (duration, codec, resolution, etc.).
 */
interface GetVideoMetadataUseCase {
    suspend operator fun invoke(itemId: String): Result<VideoMetadata>
}

@Factory(binds = [GetVideoMetadataUseCase::class])
class GetVideoMetadataUseCaseImpl(
    private val metadataRepository: MetadataRepository,
) : GetVideoMetadataUseCase {

    override suspend operator fun invoke(itemId: String): Result<VideoMetadata> =
        metadataRepository.getVideoMetadata(itemId)
}
