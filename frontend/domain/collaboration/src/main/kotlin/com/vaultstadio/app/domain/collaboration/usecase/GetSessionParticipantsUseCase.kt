/**
 * Use case for getting session participants.
 */

package com.vaultstadio.app.domain.collaboration.usecase

import com.vaultstadio.app.domain.collaboration.model.CollaborationParticipant
import com.vaultstadio.app.domain.result.Result

interface GetSessionParticipantsUseCase {
    suspend operator fun invoke(sessionId: String): Result<List<CollaborationParticipant>>
}
