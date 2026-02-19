/**
 * Delete Document Comment Use Case
 */

package com.vaultstadio.app.domain.usecase.collaboration

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.CollaborationRepository
import org.koin.core.annotation.Factory

/**
 * Use case for deleting a comment on a document.
 */
interface DeleteDocumentCommentUseCase {
    suspend operator fun invoke(itemId: String, commentId: String): Result<Unit>
}

@Factory(binds = [DeleteDocumentCommentUseCase::class])
class DeleteDocumentCommentUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : DeleteDocumentCommentUseCase {

    override suspend operator fun invoke(itemId: String, commentId: String): Result<Unit> =
        collaborationRepository.deleteComment(itemId, commentId)
}
