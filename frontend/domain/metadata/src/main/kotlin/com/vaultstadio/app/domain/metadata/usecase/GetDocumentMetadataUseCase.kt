/**
 * Use case for getting document metadata (page count, author, etc.).
 */

package com.vaultstadio.app.domain.metadata.usecase

import com.vaultstadio.app.domain.metadata.model.DocumentMetadata
import com.vaultstadio.app.domain.result.Result

interface GetDocumentMetadataUseCase {
    suspend operator fun invoke(itemId: String): Result<DocumentMetadata>
}
