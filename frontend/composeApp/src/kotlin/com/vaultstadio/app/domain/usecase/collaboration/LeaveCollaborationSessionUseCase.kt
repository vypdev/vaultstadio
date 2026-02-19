/**
 * Leave Collaboration Session Use Case
 */

package com.vaultstadio.app.domain.usecase.collaboration

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.CollaborationRepository
/**
 * Use case for leaving a collaboration session.
 */
interface LeaveCollaborationSessionUseCase {
    suspend operator fun invoke(sessionId: String): Result<Unit>
}

class LeaveCollaborationSessionUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : LeaveCollaborationSessionUseCase {

    override suspend operator fun invoke(sessionId: String): Result<Unit> =
        collaborationRepository.leaveSession(sessionId)
}
