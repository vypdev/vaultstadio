/**
 * Resolve Document Comment Use Case
 */

package com.vaultstadio.app.domain.usecase.collaboration

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.CollaborationRepository
import org.koin.core.annotation.Factory

/**
 * Use case for resolving a comment on a document.
 */
interface ResolveDocumentCommentUseCase {
    suspend operator fun invoke(itemId: String, commentId: String): ApiResult<Unit>
}

@Factory(binds = [ResolveDocumentCommentUseCase::class])
class ResolveDocumentCommentUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : ResolveDocumentCommentUseCase {

    override suspend operator fun invoke(itemId: String, commentId: String): ApiResult<Unit> =
        collaborationRepository.resolveComment(itemId, commentId)
}
