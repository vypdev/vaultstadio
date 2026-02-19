/**
 * Get File Metadata Use Case
 */

package com.vaultstadio.app.domain.usecase.metadata

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.MetadataRepository
import com.vaultstadio.app.domain.model.FileMetadata
import org.koin.core.annotation.Factory

/**
 * Use case for getting file metadata.
 */
interface GetFileMetadataUseCase {
    suspend operator fun invoke(itemId: String): ApiResult<FileMetadata>
}

@Factory(binds = [GetFileMetadataUseCase::class])
class GetFileMetadataUseCaseImpl(
    private val metadataRepository: MetadataRepository,
) : GetFileMetadataUseCase {

    override suspend operator fun invoke(itemId: String): ApiResult<FileMetadata> =
        metadataRepository.getFileMetadata(itemId)
}
