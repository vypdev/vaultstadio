/**
 * Leave Collaboration Session Use Case
 */

package com.vaultstadio.app.domain.usecase.collaboration

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.CollaborationRepository
import org.koin.core.annotation.Factory

/**
 * Use case for leaving a collaboration session.
 */
interface LeaveCollaborationSessionUseCase {
    suspend operator fun invoke(sessionId: String): ApiResult<Unit>
}

@Factory(binds = [LeaveCollaborationSessionUseCase::class])
class LeaveCollaborationSessionUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : LeaveCollaborationSessionUseCase {

    override suspend operator fun invoke(sessionId: String): ApiResult<Unit> =
        collaborationRepository.leaveSession(sessionId)
}
