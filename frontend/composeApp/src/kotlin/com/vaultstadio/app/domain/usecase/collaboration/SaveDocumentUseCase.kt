/**
 * Save Document Use Case
 */

package com.vaultstadio.app.domain.usecase.collaboration

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.CollaborationRepository
/**
 * Use case for saving a collaborative document.
 */
interface SaveDocumentUseCase {
    suspend operator fun invoke(itemId: String): Result<Unit>
}

class SaveDocumentUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : SaveDocumentUseCase {

    override suspend operator fun invoke(itemId: String): Result<Unit> =
        collaborationRepository.saveDocument(itemId)
}
