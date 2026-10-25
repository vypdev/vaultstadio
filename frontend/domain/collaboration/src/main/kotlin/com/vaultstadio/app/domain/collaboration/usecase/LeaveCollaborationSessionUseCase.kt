/**
 * Use case for leaving a collaboration session.
 */

package com.vaultstadio.app.domain.collaboration.usecase

import com.vaultstadio.app.domain.result.Result

interface LeaveCollaborationSessionUseCase {
    suspend operator fun invoke(sessionId: String): Result<Unit>
}
