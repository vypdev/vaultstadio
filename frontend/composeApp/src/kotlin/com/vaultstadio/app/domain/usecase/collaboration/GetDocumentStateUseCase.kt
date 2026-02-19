/**
 * Get Document State Use Case
 */

package com.vaultstadio.app.domain.usecase.collaboration

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.CollaborationRepository
import com.vaultstadio.app.domain.model.DocumentState
/**
 * Use case for getting the current state of a collaborative document.
 */
interface GetDocumentStateUseCase {
    suspend operator fun invoke(itemId: String): Result<DocumentState>
}

class GetDocumentStateUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : GetDocumentStateUseCase {

    override suspend operator fun invoke(itemId: String): Result<DocumentState> =
        collaborationRepository.getDocumentState(itemId)
}
