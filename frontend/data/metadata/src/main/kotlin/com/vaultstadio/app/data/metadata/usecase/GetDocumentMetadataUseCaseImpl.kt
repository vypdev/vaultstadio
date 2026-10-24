/**
 * Get document metadata use case implementation.
 */

package com.vaultstadio.app.data.metadata.usecase

import com.vaultstadio.app.domain.metadata.MetadataRepository
import com.vaultstadio.app.domain.metadata.model.DocumentMetadata
import com.vaultstadio.app.domain.metadata.usecase.GetDocumentMetadataUseCase
import com.vaultstadio.app.domain.result.Result

class GetDocumentMetadataUseCaseImpl(
    private val metadataRepository: MetadataRepository,
) : GetDocumentMetadataUseCase {

    override suspend operator fun invoke(itemId: String): Result<DocumentMetadata> =
        metadataRepository.getDocumentMetadata(itemId)
}
