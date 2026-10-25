/**
 * Get file metadata use case implementation.
 */

package com.vaultstadio.app.data.metadata.usecase

import com.vaultstadio.app.domain.metadata.MetadataRepository
import com.vaultstadio.app.domain.metadata.model.FileMetadata
import com.vaultstadio.app.domain.metadata.usecase.GetFileMetadataUseCase
import com.vaultstadio.app.domain.result.Result

class GetFileMetadataUseCaseImpl(
    private val metadataRepository: MetadataRepository,
) : GetFileMetadataUseCase {

    override suspend operator fun invoke(itemId: String): Result<FileMetadata> =
        metadataRepository.getFileMetadata(itemId)
}
