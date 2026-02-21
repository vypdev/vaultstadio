/**
 * Get Document State Use Case implementation
 */

package com.vaultstadio.app.data.collaboration.usecase

import com.vaultstadio.app.domain.collaboration.CollaborationRepository
import com.vaultstadio.app.domain.collaboration.usecase.GetDocumentStateUseCase

class GetDocumentStateUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : GetDocumentStateUseCase {
    override suspend operator fun invoke(itemId: String) =
        collaborationRepository.getDocumentState(itemId)
}
