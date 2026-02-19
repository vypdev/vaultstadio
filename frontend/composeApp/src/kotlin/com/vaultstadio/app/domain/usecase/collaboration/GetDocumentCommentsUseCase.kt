/**
 * Get Document Comments Use Case
 */

package com.vaultstadio.app.domain.usecase.collaboration

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.CollaborationRepository
import com.vaultstadio.app.domain.model.DocumentComment
/**
 * Use case for getting comments on a document.
 */
interface GetDocumentCommentsUseCase {
    suspend operator fun invoke(itemId: String, includeResolved: Boolean = false): Result<List<DocumentComment>>
}

class GetDocumentCommentsUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : GetDocumentCommentsUseCase {

    override suspend operator fun invoke(itemId: String, includeResolved: Boolean): Result<List<DocumentComment>> =
        collaborationRepository.getComments(itemId, includeResolved)
}
