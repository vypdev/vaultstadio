package com.vaultstadio.app.data.collaboration.usecase

import com.vaultstadio.app.domain.collaboration.CollaborationRepository
import com.vaultstadio.app.domain.collaboration.usecase.SetOfflineUseCase

class SetOfflineUseCaseImpl(private val collaborationRepository: CollaborationRepository) :
    SetOfflineUseCase {
    override suspend fun invoke() = collaborationRepository.setOffline()
}
