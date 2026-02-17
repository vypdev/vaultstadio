/**
 * Set Offline Use Case
 */

package com.vaultstadio.app.domain.usecase.collaboration

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.CollaborationRepository
import org.koin.core.annotation.Factory

/**
 * Use case for setting user presence to offline.
 */
interface SetOfflineUseCase {
    suspend operator fun invoke(): ApiResult<Unit>
}

@Factory(binds = [SetOfflineUseCase::class])
class SetOfflineUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : SetOfflineUseCase {

    override suspend operator fun invoke(): ApiResult<Unit> =
        collaborationRepository.setOffline()
}
