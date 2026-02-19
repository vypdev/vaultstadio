/**
 * Join Collaboration Session Use Case
 */

package com.vaultstadio.app.domain.usecase.collaboration

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.CollaborationRepository
import com.vaultstadio.app.domain.model.CollaborationSession
/**
 * Use case for joining a collaboration session.
 */
interface JoinCollaborationSessionUseCase {
    suspend operator fun invoke(itemId: String): Result<CollaborationSession>
}

class JoinCollaborationSessionUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : JoinCollaborationSessionUseCase {

    override suspend operator fun invoke(itemId: String): Result<CollaborationSession> =
        collaborationRepository.joinSession(itemId)
}
