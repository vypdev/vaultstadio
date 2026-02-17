/**
 * Get Document Comments Use Case
 */

package com.vaultstadio.app.domain.usecase.collaboration

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.CollaborationRepository
import com.vaultstadio.app.domain.model.DocumentComment
import org.koin.core.annotation.Factory

/**
 * Use case for getting comments on a document.
 */
interface GetDocumentCommentsUseCase {
    suspend operator fun invoke(itemId: String, includeResolved: Boolean = false): ApiResult<List<DocumentComment>>
}

@Factory(binds = [GetDocumentCommentsUseCase::class])
class GetDocumentCommentsUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : GetDocumentCommentsUseCase {

    override suspend operator fun invoke(itemId: String, includeResolved: Boolean): ApiResult<List<DocumentComment>> =
        collaborationRepository.getComments(itemId, includeResolved)
}
