/**
 * Leave Collaboration Session Use Case implementation
 */

package com.vaultstadio.app.data.collaboration.usecase

import com.vaultstadio.app.domain.collaboration.CollaborationRepository
import com.vaultstadio.app.domain.collaboration.usecase.LeaveCollaborationSessionUseCase

class LeaveCollaborationSessionUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : LeaveCollaborationSessionUseCase {
    override suspend operator fun invoke(sessionId: String) =
        collaborationRepository.leaveSession(sessionId)
}
