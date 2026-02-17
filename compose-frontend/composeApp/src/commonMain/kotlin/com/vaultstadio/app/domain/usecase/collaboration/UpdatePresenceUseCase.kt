/**
 * Update Presence Use Case
 */

package com.vaultstadio.app.domain.usecase.collaboration

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.CollaborationRepository
import com.vaultstadio.app.domain.model.PresenceStatus
import org.koin.core.annotation.Factory

/**
 * Use case for updating user presence status.
 */
interface UpdatePresenceUseCase {
    suspend operator fun invoke(status: PresenceStatus, activeDocument: String? = null): ApiResult<Unit>
}

@Factory(binds = [UpdatePresenceUseCase::class])
class UpdatePresenceUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : UpdatePresenceUseCase {

    override suspend operator fun invoke(status: PresenceStatus, activeDocument: String?): ApiResult<Unit> =
        collaborationRepository.updatePresence(status, activeDocument)
}
