package com.vaultstadio.app.data.collaboration.usecase

import com.vaultstadio.app.domain.collaboration.CollaborationRepository
import com.vaultstadio.app.domain.collaboration.model.PresenceStatus
import com.vaultstadio.app.domain.collaboration.usecase.UpdatePresenceUseCase

class UpdatePresenceUseCaseImpl(private val collaborationRepository: CollaborationRepository) :
    UpdatePresenceUseCase {
    override suspend fun invoke(status: PresenceStatus, activeDocument: String?) =
        collaborationRepository.updatePresence(status, activeDocument)
}
