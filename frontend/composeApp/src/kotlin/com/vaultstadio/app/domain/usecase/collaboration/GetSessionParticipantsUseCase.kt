/**
 * Get Session Participants Use Case
 */

package com.vaultstadio.app.domain.usecase.collaboration

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.CollaborationRepository
import com.vaultstadio.app.domain.model.CollaborationParticipant
import org.koin.core.annotation.Factory

/**
 * Use case for getting participants in a collaboration session.
 */
interface GetSessionParticipantsUseCase {
    suspend operator fun invoke(sessionId: String): Result<List<CollaborationParticipant>>
}

@Factory(binds = [GetSessionParticipantsUseCase::class])
class GetSessionParticipantsUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : GetSessionParticipantsUseCase {

    override suspend operator fun invoke(sessionId: String): Result<List<CollaborationParticipant>> =
        collaborationRepository.getParticipants(sessionId)
}
