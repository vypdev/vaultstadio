package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.domain.collaboration.repository.CollaborationScopeRepository
import com.vaultstadio.domain.collaboration.repository.CollaborationSessionScope
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.infrastructure.persistence.entities.CollaborationParticipantsTable
import com.vaultstadio.infrastructure.persistence.entities.CollaborationSessionsTable
import com.vaultstadio.infrastructure.persistence.entities.DocumentCommentsTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import java.sql.SQLException

/** Only scope columns are read before the application's canonical ownership decision. */
class ExposedCollaborationScopeRepository : CollaborationScopeRepository {
    override suspend fun session(sessionId: String): Either<StorageException, CollaborationSessionScope?> = scopeQuery {
        CollaborationSessionsTable.select(CollaborationSessionsTable.itemId, CollaborationSessionsTable.closedAt)
            .where { CollaborationSessionsTable.id eq sessionId }.singleOrNull()?.let {
                CollaborationSessionScope(
                    it[CollaborationSessionsTable.itemId],
                    it[CollaborationSessionsTable.closedAt] != null,
                )
            }
    }

    override suspend fun participantSession(participantId: String): Either<StorageException, String?> = scopeQuery {
        CollaborationParticipantsTable.select(CollaborationParticipantsTable.sessionId)
            .where { CollaborationParticipantsTable.id eq participantId }
            .singleOrNull()?.get(CollaborationParticipantsTable.sessionId)
    }

    override suspend fun commentItem(commentId: String): Either<StorageException, String?> = scopeQuery {
        DocumentCommentsTable.select(DocumentCommentsTable.itemId)
            .where { DocumentCommentsTable.id eq commentId }.singleOrNull()?.get(DocumentCommentsTable.itemId)
    }
}

private suspend fun <T> scopeQuery(query: () -> T): Either<StorageException, T> = try {
    collaborationDbQuery { query() }.right()
} catch (error: SQLException) {
    DatabaseException("Failed to resolve collaboration scope", error).left()
}
