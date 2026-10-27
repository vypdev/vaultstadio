package com.vaultstadio.infrastructure.persistence

import com.vaultstadio.core.domain.model.CollaborationParticipant
import com.vaultstadio.core.domain.model.CollaborationSession
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.InvalidOperationException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.infrastructure.persistence.entities.CollaborationParticipantsTable
import com.vaultstadio.infrastructure.persistence.entities.CollaborationSessionsTable
import com.vaultstadio.infrastructure.persistence.entities.StorageItemsTable
import kotlinx.datetime.Instant
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

/** All membership transactions acquire the item lock before the session lock. */
internal fun lockCollaborationItem(item: String, actor: String) {
    if (actor.isBlank()) throw AuthorizationException()
    val row = StorageItemsTable.select(StorageItemsTable.ownerId, StorageItemsTable.type, StorageItemsTable.isTrashed)
        .where { StorageItemsTable.id eq item }.forUpdate().singleOrNull() ?: throw ItemNotFoundException()
    if (row[StorageItemsTable.ownerId] != actor) throw AuthorizationException()
    if (row[StorageItemsTable.type] != "FILE" || row[StorageItemsTable.isTrashed]) {
        throw InvalidOperationException("collaboration", "An active file is required")
    }
}

internal fun readCollaborationParticipants(session: String): List<CollaborationParticipant> =
    CollaborationParticipantsTable.selectAll().where {
        CollaborationParticipantsTable.sessionId eq session and CollaborationParticipantsTable.leftAt.isNull()
    }.map { it.toCollaborationParticipant() }

internal fun lockCollaborationSession(item: String, session: String, at: Instant? = null): CollaborationSession {
    val row = CollaborationSessionsTable.selectAll().where {
        CollaborationSessionsTable.id eq session and (CollaborationSessionsTable.itemId eq item)
    }.forUpdate().singleOrNull() ?: throw ItemNotFoundException()
    if (at != null &&
        (
            row[CollaborationSessionsTable.closedAt] != null ||
                row[CollaborationSessionsTable.expiresAt] <= at
            )
    ) {
        throw InvalidOperationException("collaboration", "Session is not active")
    }
    return row.toCollaborationSession(readCollaborationParticipants(session))
}

internal fun activeCollaborationSession(item: String, at: Instant): CollaborationSession? {
    val rows = CollaborationSessionsTable.selectAll().where {
        CollaborationSessionsTable.itemId eq item and CollaborationSessionsTable.closedAt.isNull() and
            (CollaborationSessionsTable.expiresAt greater at)
    }.forUpdate().toList()
    if (rows.size > 1) throw DatabaseException("Multiple active collaboration sessions for one item")
    return rows.singleOrNull()?.let { row ->
        row.toCollaborationSession(readCollaborationParticipants(row[CollaborationSessionsTable.id]))
    }
}

internal fun insertCollaborationSession(session: CollaborationSession) {
    CollaborationSessionsTable.insert {
        it[id] = session.id
        it[itemId] = session.itemId
        it[createdAt] = session.createdAt
        it[expiresAt] = session.expiresAt
    }
}

internal fun insertCollaborationParticipant(session: String, participant: CollaborationParticipant) {
    CollaborationParticipantsTable.insert {
        it[id] = participant.id
        it[sessionId] = session
        it[userId] = participant.userId
        it[userName] = participant.userName
        it[color] = participant.color
        it[joinedAt] = participant.joinedAt
        it[lastActiveAt] = participant.lastActiveAt
        it[isEditing] = participant.isEditing
    }
}

internal fun updateCollaborationParticipant(session: String, participant: CollaborationParticipant) {
    CollaborationParticipantsTable.update({
        CollaborationParticipantsTable.id eq participant.id and (CollaborationParticipantsTable.sessionId eq session)
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
