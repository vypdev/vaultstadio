/**
 * Get Document State Use Case
 */

package com.vaultstadio.app.domain.usecase.collaboration

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.CollaborationRepository
import com.vaultstadio.app.domain.model.DocumentState
import org.koin.core.annotation.Factory

/**
 * Use case for getting the current state of a collaborative document.
 */
interface GetDocumentStateUseCase {
    suspend operator fun invoke(itemId: String): ApiResult<DocumentState>
}

@Factory(binds = [GetDocumentStateUseCase::class])
class GetDocumentStateUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : GetDocumentStateUseCase {

    override suspend operator fun invoke(itemId: String): ApiResult<DocumentState> =
        collaborationRepository.getDocumentState(itemId)
}
