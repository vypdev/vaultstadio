package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.right
import com.vaultstadio.core.domain.model.CollaborationParticipant
import com.vaultstadio.core.domain.model.CollaborationSession
import com.vaultstadio.domain.collaboration.repository.CollaborationAdmission
import com.vaultstadio.domain.collaboration.repository.CollaborationMembershipRepository
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.infrastructure.persistence.entities.CollaborationParticipantsTable
import com.vaultstadio.infrastructure.persistence.entities.CollaborationSessionsTable
import com.vaultstadio.infrastructure.persistence.entities.StorageItemsTable
import kotlinx.datetime.Instant
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inSubQuery
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update

/** Atomic effects use authoritative ownership and membership checks in the same SQL transaction. */
class ExposedCollaborationMembershipRepository : CollaborationMembershipRepository {
    override suspend fun admit(
        itemId: String,
        actorId: String,
        at: Instant,
        prepare: CollaborationAdmission,
    ): Either<StorageException, Pair<CollaborationSession, CollaborationParticipant>> =
        collaborationRepositoryCall("Failed to admit collaboration participant") {
            collaborationDbQuery {
                lockCollaborationItem(itemId, actorId)
                val existing = activeCollaborationSession(itemId, at)
                val (session, participant) = prepare(existing).fold({ throw it }, { it })
                validateAdmission(itemId, actorId, existing, session, participant)
                if (existing == null) insertCollaborationSession(session)
                insertCollaborationParticipant(session.id, participant)
                val persisted = lockCollaborationSession(itemId, session.id, at)
                Pair(persisted, persisted.participants.single { it.id == participant.id })
            }.right()
        }

    private fun validateAdmission(
        item: String,
        actor: String,
        existing: CollaborationSession?,
        planned: CollaborationSession,
        participant: CollaborationParticipant,
    ) {
        if (planned.itemId != item || planned.id.isBlank()) throw DatabaseException("Inconsistent admission session")
        if (participant.userId != actor ||
            participant.id.isBlank()
        ) {
            throw DatabaseException("Inconsistent admission actor")
        }
        if (existing != null && planned != existing) throw DatabaseException("Inconsistent existing session")
        if (existing == null &&
            planned.participants.isNotEmpty()
        ) {
            throw DatabaseException("Unexpected initial participants")
        }
    }

    override suspend fun leave(
        itemId: String,
        sessionId: String,
        participantId: String,
        actorId: String,
        at: Instant,
    ): Either<StorageException, Unit> = collaborationRepositoryCall("Failed to leave collaboration session") {
        collaborationDbQuery {
            lockCollaborationItem(itemId, actorId)
            val session = lockCollaborationSession(itemId, sessionId)
            ownParticipant(session, participantId, actorId)
            CollaborationParticipantsTable.update({
                CollaborationParticipantsTable.id eq participantId and
                    (CollaborationParticipantsTable.sessionId eq sessionId) and
                    (CollaborationParticipantsTable.userId eq actorId) and
                    CollaborationParticipantsTable.leftAt.isNull()
            }) { it[leftAt] = at }
            if (readCollaborationParticipants(sessionId).isEmpty()) {
                CollaborationSessionsTable.update({ CollaborationSessionsTable.id eq sessionId }) { it[closedAt] = at }
            }
        }
        Unit.right()
    }

    override suspend fun update(
        itemId: String,
        sessionId: String,
        participant: CollaborationParticipant,
        actorId: String,
        at: Instant,
    ): Either<StorageException, CollaborationParticipant> =
        collaborationRepositoryCall("Failed to update collaboration participant") {
            collaborationDbQuery {
                lockCollaborationItem(itemId, actorId)
                val session = lockCollaborationSession(itemId, sessionId, at)
                val existing = ownParticipant(session, participant.id, actorId)
                val identity = participant.copy(
                    cursor = existing.cursor,
                    selection = existing.selection,
                    lastActiveAt = existing.lastActiveAt,
                    isEditing = existing.isEditing,
                )
                if (identity != existing) throw AuthorizationException()
                updateCollaborationParticipant(sessionId, participant)
                readCollaborationParticipants(sessionId).single { it.id == participant.id }
            }.right()
        }

    override suspend fun closeExpiredForOwner(actorId: String, at: Instant): Either<StorageException, Int> =
        collaborationRepositoryCall("Failed to close expired collaboration sessions") {
            if (actorId.isBlank()) throw AuthorizationException()
            collaborationDbQuery {
                val ownedItems = StorageItemsTable.select(StorageItemsTable.id).where {
                    StorageItemsTable.ownerId eq
                        actorId
                }
                CollaborationSessionsTable.update({
                    CollaborationSessionsTable.itemId inSubQuery ownedItems and
                        (CollaborationSessionsTable.expiresAt lessEq at) and
                        CollaborationSessionsTable.closedAt.isNull()
                }) { it[closedAt] = at }
            }.right()
        }

    private fun ownParticipant(session: CollaborationSession, id: String, actor: String): CollaborationParticipant {
        val participant = session.participants.singleOrNull { it.id == id } ?: throw ItemNotFoundException()
        if (participant.userId != actor) throw AuthorizationException()
        return participant
    }
}
