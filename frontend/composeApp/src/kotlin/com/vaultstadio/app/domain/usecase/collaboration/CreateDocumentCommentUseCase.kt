/**
 * Create Document Comment Use Case
 */

package com.vaultstadio.app.domain.usecase.collaboration

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.CollaborationRepository
import org.koin.core.annotation.Factory

/**
 * Use case for creating a comment on a document.
 */
interface CreateDocumentCommentUseCase {
    suspend operator fun invoke(
        itemId: String,
        content: String,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
    ): Result<Unit>
}

@Factory(binds = [CreateDocumentCommentUseCase::class])
class CreateDocumentCommentUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : CreateDocumentCommentUseCase {

    override suspend operator fun invoke(
        itemId: String,
        content: String,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
    ): Result<Unit> = collaborationRepository.createComment(
        itemId,
        content,
        startLine,
        startColumn,
        endLine,
        endColumn,
    )
}
