package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.CollaborationParticipant
import com.vaultstadio.core.domain.model.CollaborationSession
import com.vaultstadio.core.domain.repository.CollaborationParticipantsRepository
import com.vaultstadio.core.domain.repository.CollaborationSessionsRepository
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.infrastructure.persistence.entities.CollaborationParticipantsTable
import com.vaultstadio.infrastructure.persistence.entities.CollaborationSessionsTable
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

internal class ExposedCollaborationSessions : CollaborationSessionsRepository, CollaborationParticipantsRepository {
    override suspend fun createSession(session: CollaborationSession): Either<StorageException, CollaborationSession> {
        return collaborationRepositoryCall("Failed to create session") {
            collaborationDbQuery {
                CollaborationSessionsTable.insert {
                    it[id] = session.id
                    it[itemId] = session.itemId
                    it[createdAt] = session.createdAt
                    it[expiresAt] = session.expiresAt
                }
            }
            session.right()
        }
    }

    override suspend fun findSession(sessionId: String): Either<StorageException, CollaborationSession?> {
        return collaborationRepositoryCall("Failed to find session") {
            collaborationDbQuery {
                CollaborationSessionsTable.selectAll()
                    .where { CollaborationSessionsTable.id eq sessionId }
                    .map { row ->
                        val participants = getParticipantsForSession(sessionId)
                        row.toCollaborationSession(participants)
                    }
                    .singleOrNull()
            }.right()
        }
    }

    override suspend fun findActiveSessionForItem(itemId: String): Either<StorageException, CollaborationSession?> {
        return collaborationRepositoryCall("Failed to find active session") {
            collaborationDbQuery {
                val now = Clock.System.now()
                CollaborationSessionsTable.selectAll()
                    .where {
                        CollaborationSessionsTable.itemId eq itemId and
                            CollaborationSessionsTable.closedAt.isNull() and
                            (CollaborationSessionsTable.expiresAt greater now)
                    }
                    .map { row ->
                        val participants = getParticipantsForSession(row[CollaborationSessionsTable.id])
                        row.toCollaborationSession(participants)
                    }
                    .singleOrNull()
            }.right()
        }
    }

    override suspend fun updateSession(session: CollaborationSession): Either<StorageException, CollaborationSession> {
        return collaborationRepositoryCall("Failed to update session") {
            collaborationDbQuery {
                CollaborationSessionsTable.update({ CollaborationSessionsTable.id eq session.id }) {
                    it[expiresAt] = session.expiresAt
                }
            }
            session.right()
        }
    }

    override suspend fun closeSession(sessionId: String): Either<StorageException, Unit> {
        return collaborationRepositoryCall("Failed to close session") {
            collaborationDbQuery {
                CollaborationSessionsTable.update({ CollaborationSessionsTable.id eq sessionId }) {
                    it[closedAt] = Clock.System.now()
                }
            }
            Unit.right()
        }
    }

    override suspend fun listUserSessions(userId: String): Either<StorageException, List<CollaborationSession>> {
        return collaborationRepositoryCall("Failed to list user sessions") {
            collaborationDbQuery {
                val now = Clock.System.now()
                val sessionIds = CollaborationParticipantsTable.selectAll()
                    .where {
                        CollaborationParticipantsTable.userId eq userId and
                            CollaborationParticipantsTable.leftAt.isNull()
                    }
                    .map { it[CollaborationParticipantsTable.sessionId] }
                    .distinct()

                CollaborationSessionsTable.selectAll()
                    .where {
                        CollaborationSessionsTable.id inList sessionIds and
                            CollaborationSessionsTable.closedAt.isNull() and
                            (CollaborationSessionsTable.expiresAt greater now)
                    }
                    .map { row ->
                        val participants = getParticipantsForSession(row[CollaborationSessionsTable.id])
                        row.toCollaborationSession(participants)
                    }
            }.right()
        }
    }

    override suspend fun cleanupExpiredSessions(expiredBefore: Instant): Either<StorageException, Int> {
        return collaborationRepositoryCall("Failed to cleanup sessions") {
            collaborationDbQuery {
                CollaborationSessionsTable.update({
                    CollaborationSessionsTable.expiresAt less expiredBefore and
                        CollaborationSessionsTable.closedAt.isNull()
                }) {
                    it[closedAt] = Clock.System.now()
                }
            }.right()
        }
    }

    override suspend fun addParticipant(
        sessionId: String,
        participant: CollaborationParticipant,
    ): Either<StorageException, CollaborationSession> {
        return collaborationRepositoryCall("Failed to add participant") {
            collaborationDbQuery {
                CollaborationParticipantsTable.insert {
                    it[id] = participant.id
                    it[CollaborationParticipantsTable.sessionId] = sessionId
                    it[userId] = participant.userId
                    it[userName] = participant.userName
                    it[color] = participant.color
                    it[joinedAt] = participant.joinedAt
                    it[lastActiveAt] = participant.lastActiveAt
                    it[isEditing] = participant.isEditing
                }
            }
            findSession(sessionId).fold(
                { it.left() },
                { session -> session?.right() ?: DatabaseException("Session not found").left() },
            )
        }
    }

    override suspend fun removeParticipant(
        sessionId: String,
        participantId: String,
    ): Either<StorageException, CollaborationSession> {
        return collaborationRepositoryCall("Failed to remove participant") {
            collaborationDbQuery {
                CollaborationParticipantsTable.update({
                    CollaborationParticipantsTable.sessionId eq sessionId and
                        (CollaborationParticipantsTable.id eq participantId)
                }) {
                    it[leftAt] = Clock.System.now()
                }
            }
            findSession(sessionId).fold(
                { it.left() },
                { session -> session?.right() ?: DatabaseException("Session not found").left() },
            )
        }
    }

    override suspend fun updateParticipant(
        sessionId: String,
        participant: CollaborationParticipant,
    ): Either<StorageException, CollaborationParticipant> {
        return collaborationRepositoryCall("Failed to update participant") {
            collaborationDbQuery {
                CollaborationParticipantsTable.update({
                    CollaborationParticipantsTable.id eq participant.id
                }) {
                    it[cursorLine] = participant.cursor?.line
                    it[cursorColumn] = participant.cursor?.column
                    it[cursorOffset] = participant.cursor?.offset
                    it[selectionStartLine] = participant.selection?.start?.line
                    it[selectionStartColumn] = participant.selection?.start?.column
                    it[selectionStartOffset] = participant.selection?.start?.offset
                    it[selectionEndLine] = participant.selection?.end?.line
                    it[selectionEndColumn] = participant.selection?.end?.column
                    it[selectionEndOffset] = participant.selection?.end?.offset
                    it[lastActiveAt] = participant.lastActiveAt
                    it[isEditing] = participant.isEditing
                }
            }
            participant.right()
        }
    }

    override suspend fun getParticipants(sessionId: String): Either<StorageException, List<CollaborationParticipant>> {
        return collaborationRepositoryCall("Failed to get participants") {
            collaborationDbQuery { getParticipantsForSession(sessionId) }.right()
        }
    }

    private fun getParticipantsForSession(sessionId: String): List<CollaborationParticipant> {
        return CollaborationParticipantsTable.selectAll()
            .where {
                CollaborationParticipantsTable.sessionId eq sessionId and
                    CollaborationParticipantsTable.leftAt.isNull()
            }
            .map { it.toCollaborationParticipant() }
    }
}
