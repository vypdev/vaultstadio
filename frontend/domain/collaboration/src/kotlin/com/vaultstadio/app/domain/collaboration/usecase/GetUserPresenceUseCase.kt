/**
 * Use case for getting user presence.
 */

package com.vaultstadio.app.domain.collaboration.usecase

import com.vaultstadio.app.domain.collaboration.model.UserPresence
import com.vaultstadio.app.domain.result.Result

interface GetUserPresenceUseCase {
    suspend operator fun invoke(userIds: List<String>): Result<List<UserPresence>>
}
