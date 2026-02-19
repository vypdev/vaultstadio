/**
 * Get Collaboration Session Use Case
 */

package com.vaultstadio.app.domain.usecase.collaboration

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.CollaborationRepository
import com.vaultstadio.app.domain.model.CollaborationSession
import org.koin.core.annotation.Factory

/**
 * Use case for getting details of an existing collaboration session.
 */
interface GetCollaborationSessionUseCase {
    suspend operator fun invoke(sessionId: String): Result<CollaborationSession>
}

@Factory(binds = [GetCollaborationSessionUseCase::class])
class GetCollaborationSessionUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : GetCollaborationSessionUseCase {

    override suspend operator fun invoke(sessionId: String): Result<CollaborationSession> =
        collaborationRepository.getSession(sessionId)
}
