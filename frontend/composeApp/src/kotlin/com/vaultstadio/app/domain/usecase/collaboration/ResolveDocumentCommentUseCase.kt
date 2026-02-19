/**
 * Resolve Document Comment Use Case
 */

package com.vaultstadio.app.domain.usecase.collaboration

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.CollaborationRepository
/**
 * Use case for resolving a comment on a document.
 */
interface ResolveDocumentCommentUseCase {
    suspend operator fun invoke(itemId: String, commentId: String): Result<Unit>
}

class ResolveDocumentCommentUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : ResolveDocumentCommentUseCase {

    override suspend operator fun invoke(itemId: String, commentId: String): Result<Unit> =
        collaborationRepository.resolveComment(itemId, commentId)
}
