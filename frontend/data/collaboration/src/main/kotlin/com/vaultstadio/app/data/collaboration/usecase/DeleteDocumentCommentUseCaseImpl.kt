package com.vaultstadio.app.data.collaboration.usecase

import com.vaultstadio.app.domain.collaboration.CollaborationRepository
import com.vaultstadio.app.domain.collaboration.usecase.DeleteDocumentCommentUseCase

class DeleteDocumentCommentUseCaseImpl(private val collaborationRepository: CollaborationRepository) :
    DeleteDocumentCommentUseCase {
    override suspend fun invoke(itemId: String, commentId: String) =
        collaborationRepository.deleteComment(itemId, commentId)
}
