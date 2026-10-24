/**
 * Save Document Use Case implementation
 */

package com.vaultstadio.app.data.collaboration.usecase

import com.vaultstadio.app.domain.collaboration.CollaborationRepository
import com.vaultstadio.app.domain.collaboration.usecase.SaveDocumentUseCase

class SaveDocumentUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : SaveDocumentUseCase {
    override suspend operator fun invoke(itemId: String) =
        collaborationRepository.saveDocument(itemId)
}
