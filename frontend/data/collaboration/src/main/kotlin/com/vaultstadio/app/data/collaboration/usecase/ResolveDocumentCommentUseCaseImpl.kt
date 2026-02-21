package com.vaultstadio.app.data.collaboration.usecase

import com.vaultstadio.app.domain.collaboration.CollaborationRepository
import com.vaultstadio.app.domain.collaboration.usecase.ResolveDocumentCommentUseCase

class ResolveDocumentCommentUseCaseImpl(private val collaborationRepository: CollaborationRepository) :
    ResolveDocumentCommentUseCase {
    override suspend fun invoke(itemId: String, commentId: String) =
        collaborationRepository.resolveComment(itemId, commentId)
}
