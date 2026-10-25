/**
 * Join Collaboration Session Use Case implementation
 */

package com.vaultstadio.app.data.collaboration.usecase

import com.vaultstadio.app.domain.collaboration.CollaborationRepository
import com.vaultstadio.app.domain.collaboration.usecase.JoinCollaborationSessionUseCase

class JoinCollaborationSessionUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : JoinCollaborationSessionUseCase {
    override suspend operator fun invoke(itemId: String) = collaborationRepository.joinSession(itemId)
}
