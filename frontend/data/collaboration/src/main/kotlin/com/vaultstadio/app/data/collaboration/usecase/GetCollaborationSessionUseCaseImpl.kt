/**
 * Get Collaboration Session Use Case implementation
 */

package com.vaultstadio.app.data.collaboration.usecase

import com.vaultstadio.app.domain.collaboration.CollaborationRepository
import com.vaultstadio.app.domain.collaboration.usecase.GetCollaborationSessionUseCase

class GetCollaborationSessionUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : GetCollaborationSessionUseCase {
    override suspend operator fun invoke(sessionId: String) =
        collaborationRepository.getSession(sessionId)
}
