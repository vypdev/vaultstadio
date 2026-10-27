package com.vaultstadio.domain.collaboration.repository

import arrow.core.Either
import com.vaultstadio.domain.common.exception.StorageException

/** Minimal internal facts; availability is interpreted only after canonical ownership. */
data class CollaborationSessionScope(val itemId: String, val closed: Boolean)

interface CollaborationScopeRepository {
    suspend fun session(sessionId: String): Either<StorageException, CollaborationSessionScope?>
    suspend fun participantSession(participantId: String): Either<StorageException, String?>
    suspend fun commentItem(commentId: String): Either<StorageException, String?>
}
