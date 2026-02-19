/**
 * Set Offline Use Case
 */

package com.vaultstadio.app.domain.usecase.collaboration

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.CollaborationRepository
/**
 * Use case for setting user presence to offline.
 */
interface SetOfflineUseCase {
    suspend operator fun invoke(): Result<Unit>
}

class SetOfflineUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : SetOfflineUseCase {

    override suspend operator fun invoke(): Result<Unit> =
        collaborationRepository.setOffline()
}
