/**
 * Get Document Metadata Use Case
 */

package com.vaultstadio.app.domain.usecase.metadata

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.MetadataRepository
import com.vaultstadio.app.domain.model.DocumentMetadata
import org.koin.core.annotation.Factory

/**
 * Use case for getting document metadata (page count, author, etc.).
 */
interface GetDocumentMetadataUseCase {
    suspend operator fun invoke(itemId: String): Result<DocumentMetadata>
}

@Factory(binds = [GetDocumentMetadataUseCase::class])
class GetDocumentMetadataUseCaseImpl(
    private val metadataRepository: MetadataRepository,
) : GetDocumentMetadataUseCase {

    override suspend operator fun invoke(itemId: String): Result<DocumentMetadata> =
        metadataRepository.getDocumentMetadata(itemId)
}
