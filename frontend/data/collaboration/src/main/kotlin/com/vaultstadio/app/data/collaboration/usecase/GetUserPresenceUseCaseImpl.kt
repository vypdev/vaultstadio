package com.vaultstadio.app.data.collaboration.usecase

import com.vaultstadio.app.domain.collaboration.CollaborationRepository
import com.vaultstadio.app.domain.collaboration.usecase.GetUserPresenceUseCase

class GetUserPresenceUseCaseImpl(private val collaborationRepository: CollaborationRepository) :
    GetUserPresenceUseCase {
    override suspend fun invoke(userIds: List<String>) =
        collaborationRepository.getUserPresence(userIds)
}
