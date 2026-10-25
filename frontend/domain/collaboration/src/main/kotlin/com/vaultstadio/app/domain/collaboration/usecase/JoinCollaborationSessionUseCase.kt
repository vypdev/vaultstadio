/**
 * Use case for joining a collaboration session.
 */

package com.vaultstadio.app.domain.collaboration.usecase

import com.vaultstadio.app.domain.collaboration.model.CollaborationSession
import com.vaultstadio.app.domain.result.Result

interface JoinCollaborationSessionUseCase {
    suspend operator fun invoke(itemId: String): Result<CollaborationSession>
}
