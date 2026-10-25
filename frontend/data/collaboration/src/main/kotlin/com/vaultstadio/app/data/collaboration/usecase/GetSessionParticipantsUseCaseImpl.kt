/**
 * Get Session Participants Use Case implementation
 */

package com.vaultstadio.app.data.collaboration.usecase

import com.vaultstadio.app.domain.collaboration.CollaborationRepository
import com.vaultstadio.app.domain.collaboration.usecase.GetSessionParticipantsUseCase

class GetSessionParticipantsUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : GetSessionParticipantsUseCase {
    override suspend operator fun invoke(sessionId: String) =
        collaborationRepository.getParticipants(sessionId)
}
