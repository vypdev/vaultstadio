/**
 * Get User Presence Use Case
 */

package com.vaultstadio.app.domain.usecase.collaboration

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.CollaborationRepository
import com.vaultstadio.app.domain.model.UserPresence
/**
 * Use case for getting the presence status of specified users.
 */
interface GetUserPresenceUseCase {
    suspend operator fun invoke(userIds: List<String>): Result<List<UserPresence>>
}

class GetUserPresenceUseCaseImpl(
    private val collaborationRepository: CollaborationRepository,
) : GetUserPresenceUseCase {

    override suspend operator fun invoke(userIds: List<String>): Result<List<UserPresence>> =
        collaborationRepository.getUserPresence(userIds)
}
