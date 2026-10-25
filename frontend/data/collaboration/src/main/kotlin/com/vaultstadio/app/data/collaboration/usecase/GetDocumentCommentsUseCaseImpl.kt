package com.vaultstadio.app.data.collaboration.usecase

import com.vaultstadio.app.domain.collaboration.CollaborationRepository
import com.vaultstadio.app.domain.collaboration.usecase.GetDocumentCommentsUseCase

class GetDocumentCommentsUseCaseImpl(private val collaborationRepository: CollaborationRepository) :
    GetDocumentCommentsUseCase {
    override suspend fun invoke(itemId: String, includeResolved: Boolean) =
        collaborationRepository.getComments(itemId, includeResolved)
}
