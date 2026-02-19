/**
 * Update Presence Use Case
 */

package com.vaultstadio.app.domain.usecase.collaboration

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.CollaborationRepository
import com.vaultstadio.app.domain.model.PresenceStatus
/**
 * Use case for updating user presence status.
 */
interface UpdatePresenceUseCase {
    suspend operator fun invoke(status: PresenceStatus, activeDocument: String? = null): Result<Unit>
}

class UpdatePresenceUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : UpdatePresenceUseCase {

    override suspend operator fun invoke(status: PresenceStatus, activeDocument: String?): Result<Unit> =
        collaborationRepository.updatePresence(status, activeDocument)
}
