/**
 * Use case for getting a collaboration session.
 */

package com.vaultstadio.app.domain.collaboration.usecase

import com.vaultstadio.app.domain.collaboration.model.CollaborationSession
import com.vaultstadio.app.domain.result.Result

interface GetCollaborationSessionUseCase {
    suspend operator fun invoke(sessionId: String): Result<CollaborationSession>
}
