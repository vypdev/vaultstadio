package com.vaultstadio.app.data.collaboration.usecase

import com.vaultstadio.app.domain.collaboration.CollaborationRepository
import com.vaultstadio.app.domain.collaboration.usecase.CreateDocumentCommentUseCase

class CreateDocumentCommentUseCaseImpl(private val collaborationRepository: CollaborationRepository) :
    CreateDocumentCommentUseCase {
    override suspend fun invoke(
        itemId: String,
        content: String,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
    ) = collaborationRepository.createComment(
        itemId, content, startLine, startColumn, endLine, endColumn,
    )
}
