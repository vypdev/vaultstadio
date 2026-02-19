/**
 * Get File Metadata Use Case
 */

package com.vaultstadio.app.domain.usecase.metadata

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.MetadataRepository
import com.vaultstadio.app.domain.model.FileMetadata
/**
 * Use case for getting file metadata.
 */
interface GetFileMetadataUseCase {
    suspend operator fun invoke(itemId: String): Result<FileMetadata>
}

class GetFileMetadataUseCaseImpl(
    private val metadataRepository: MetadataRepository,
) : GetFileMetadataUseCase {

    override suspend operator fun invoke(itemId: String): Result<FileMetadata> =
        metadataRepository.getFileMetadata(itemId)
}
