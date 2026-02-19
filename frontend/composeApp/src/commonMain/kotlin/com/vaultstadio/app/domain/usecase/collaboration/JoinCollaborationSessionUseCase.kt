/**
 * Join Collaboration Session Use Case
 */

package com.vaultstadio.app.domain.usecase.collaboration

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.CollaborationRepository
import com.vaultstadio.app.domain.model.CollaborationSession
import org.koin.core.annotation.Factory

/**
 * Use case for joining a collaboration session.
 */
interface JoinCollaborationSessionUseCase {
    suspend operator fun invoke(itemId: String): ApiResult<CollaborationSession>
}

@Factory(binds = [JoinCollaborationSessionUseCase::class])
class JoinCollaborationSessionUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : JoinCollaborationSessionUseCase {

    override suspend operator fun invoke(itemId: String): ApiResult<CollaborationSession> =
        collaborationRepository.joinSession(itemId)
}
