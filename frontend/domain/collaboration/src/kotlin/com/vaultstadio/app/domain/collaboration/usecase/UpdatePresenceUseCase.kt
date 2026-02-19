/**
 * Use case for updating user presence status.
 */

package com.vaultstadio.app.domain.collaboration.usecase

import com.vaultstadio.app.domain.collaboration.model.PresenceStatus
import com.vaultstadio.app.domain.result.Result

interface UpdatePresenceUseCase {
    suspend operator fun invoke(status: PresenceStatus, activeDocument: String? = null): Result<Unit>
}
