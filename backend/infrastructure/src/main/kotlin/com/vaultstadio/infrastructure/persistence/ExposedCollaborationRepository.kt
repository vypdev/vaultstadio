/**
 * VaultStadio Exposed Collaboration Repository Implementation
 */

package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.CollaborationOperation
import com.vaultstadio.core.domain.model.CollaborationParticipant
import com.vaultstadio.core.domain.model.CollaborationSession
import com.vaultstadio.core.domain.model.CommentReply
import com.vaultstadio.core.domain.model.DocumentComment
import com.vaultstadio.core.domain.model.DocumentState
import com.vaultstadio.core.domain.model.PresenceStatus
import com.vaultstadio.core.domain.model.UserPresence
import com.vaultstadio.core.domain.repository.CollaborationRepository
import com.vaultstadio.core.exception.DatabaseException
import com.vaultstadio.core.exception.StorageException
import com.vaultstadio.infrastructure.persistence.entities.CollaborationOperationsTable
import com.vaultstadio.infrastructure.persistence.entities.CollaborationParticipantsTable
import com.vaultstadio.infrastructure.persistence.entities.CollaborationSessionsTable
import com.vaultstadio.infrastructure.persistence.entities.CommentRepliesTable
import com.vaultstadio.infrastructure.persistence.entities.DocumentCommentsTable
import com.vaultstadio.infrastructure.persistence.entities.DocumentStatesTable
import com.vaultstadio.infrastructure.persistence.entities.UserPresenceTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

/**
 * Exposed implementation of CollaborationRepository.
 */
class ExposedCollaborationRepository : CollaborationRepository {

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    // ========================================================================
    // Session Management
    // ========================================================================

    override suspend fun createSession(session: CollaborationSession): Either<StorageException, CollaborationSession> {
        return try {
            dbQuery {
                CollaborationSessionsTable.insert {
                    it[id] = session.id
                    it[itemId] = session.itemId
                    it[createdAt] = session.createdAt
                    it[expiresAt] = session.expiresAt
                }
            }
            session.right()
        } catch (e: Exception) {
            DatabaseException("Failed to create session: ${e.message}", e).left()
        }
    }

    override suspend fun findSession(sessionId: String): Either<StorageException, CollaborationSession?> {
        return try {
            dbQuery {
                CollaborationSessionsTable.selectAll()
                    .where { CollaborationSessionsTable.id eq sessionId }
                    .map { row ->
                        val participants = getParticipantsForSession(sessionId)
                        row.toCollaborationSession(participants)
                    }
                    .singleOrNull()
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to find session: ${e.message}", e).left()
        }
    }

    override suspend fun findActiveSessionForItem(itemId: String): Either<StorageException, CollaborationSession?> {
        return try {
            dbQuery {
                val now = Clock.System.now()
                CollaborationSessionsTable.selectAll()
                    .where {
                        (CollaborationSessionsTable.itemId eq itemId) and
                            (CollaborationSessionsTable.closedAt.isNull()) and
                            (CollaborationSessionsTable.expiresAt greater now)
                    }
                    .map { row ->
                        val participants = getParticipantsForSession(row[CollaborationSessionsTable.id])
                        row.toCollaborationSession(participants)
                    }
                    .singleOrNull()
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to find active session: ${e.message}", e).left()
        }
    }

    override suspend fun updateSession(session: CollaborationSession): Either<StorageException, CollaborationSession> {
        return try {
            dbQuery {
                CollaborationSessionsTable.update({ CollaborationSessionsTable.id eq session.id }) {
                    it[expiresAt] = session.expiresAt
                }
            }
            session.right()
        } catch (e: Exception) {
            DatabaseException("Failed to update session: ${e.message}", e).left()
        }
    }

    override suspend fun closeSession(sessionId: String): Either<StorageException, Unit> {
        return try {
            dbQuery {
                CollaborationSessionsTable.update({ CollaborationSessionsTable.id eq sessionId }) {
                    it[closedAt] = Clock.System.now()
                }
            }
            Unit.right()
        } catch (e: Exception) {
            DatabaseException("Failed to close session: ${e.message}", e).left()
        }
    }

    override suspend fun listUserSessions(userId: String): Either<StorageException, List<CollaborationSession>> {
        return try {
            dbQuery {
                val now = Clock.System.now()
                val sessionIds = CollaborationParticipantsTable.selectAll()
                    .where {
                        (CollaborationParticipantsTable.userId eq userId) and
                            (CollaborationParticipantsTable.leftAt.isNull())
                    }
                    .map { it[CollaborationParticipantsTable.sessionId] }
                    .distinct()

                CollaborationSessionsTable.selectAll()
                    .where {
                        (CollaborationSessionsTable.id inList sessionIds) and
                            (CollaborationSessionsTable.closedAt.isNull()) and
                            (CollaborationSessionsTable.expiresAt greater now)
                    }
                    .map { row ->
                        val participants = getParticipantsForSession(row[CollaborationSessionsTable.id])
                        row.toCollaborationSession(participants)
                    }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to list user sessions: ${e.message}", e).left()
        }
    }

    override suspend fun cleanupExpiredSessions(expiredBefore: Instant): Either<StorageException, Int> {
        return try {
            dbQuery {
                CollaborationSessionsTable.update({
                    (CollaborationSessionsTable.expiresAt less expiredBefore) and
                        (CollaborationSessionsTable.closedAt.isNull())
                }) {
                    it[closedAt] = Clock.System.now()
                }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to cleanup sessions: ${e.message}", e).left()
        }
    }

    // ========================================================================
    // Participant Management
    // ========================================================================

    override suspend fun addParticipant(
        sessionId: String,
        participant: CollaborationParticipant,
    ): Either<StorageException, CollaborationSession> {
        return try {
            dbQuery {
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
        } catch (e: Exception) {
            DatabaseException("Failed to add participant: ${e.message}", e).left()
        }
    }

    override suspend fun removeParticipant(
        sessionId: String,
        participantId: String,
    ): Either<StorageException, CollaborationSession> {
        return try {
            dbQuery {
                CollaborationParticipantsTable.update({
                    (CollaborationParticipantsTable.sessionId eq sessionId) and
                        (CollaborationParticipantsTable.id eq participantId)
                }) {
                    it[leftAt] = Clock.System.now()
                }
            }
            findSession(sessionId).fold(
                { it.left() },
                { session -> session?.right() ?: DatabaseException("Session not found").left() },
            )
        } catch (e: Exception) {
            DatabaseException("Failed to remove participant: ${e.message}", e).left()
        }
    }

    override suspend fun updateParticipant(
        sessionId: String,
        participant: CollaborationParticipant,
    ): Either<StorageException, CollaborationParticipant> {
        return try {
            dbQuery {
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
        } catch (e: Exception) {
            DatabaseException("Failed to update participant: ${e.message}", e).left()
        }
    }

    override suspend fun getParticipants(sessionId: String): Either<StorageException, List<CollaborationParticipant>> {
        return try {
            dbQuery { getParticipantsForSession(sessionId) }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to get participants: ${e.message}", e).left()
        }
    }

    private fun getParticipantsForSession(sessionId: String): List<CollaborationParticipant> {
        return CollaborationParticipantsTable.selectAll()
            .where {
                (CollaborationParticipantsTable.sessionId eq sessionId) and
                    (CollaborationParticipantsTable.leftAt.isNull())
            }
            .map { it.toCollaborationParticipant() }
    }

    // ========================================================================
    // Document State & Operations
    // ========================================================================

    override suspend fun getDocumentState(itemId: String): Either<StorageException, DocumentState?> {
        return try {
            dbQuery {
                DocumentStatesTable.selectAll()
                    .where { DocumentStatesTable.itemId eq itemId }
                    .map { it.toDocumentState() }
                    .singleOrNull()
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to get document state: ${e.message}", e).left()
        }
    }

    override suspend fun saveDocumentState(state: DocumentState): Either<StorageException, DocumentState> {
        return try {
            dbQuery {
                val exists = DocumentStatesTable.selectAll()
                    .where { DocumentStatesTable.itemId eq state.itemId }
                    .count() > 0

                if (exists) {
                    DocumentStatesTable.update({ DocumentStatesTable.itemId eq state.itemId }) {
                        it[version] = state.version
                        it[content] = state.content
                        it[lastModified] = state.lastModified
                    }
                } else {
                    DocumentStatesTable.insert {
                        it[id] = java.util.UUID.randomUUID().toString()
                        it[itemId] = state.itemId
                        it[version] = state.version
                        it[content] = state.content
                        it[lastModified] = state.lastModified
                    }
                }
            }
            state.right()
        } catch (e: Exception) {
            DatabaseException("Failed to save document state: ${e.message}", e).left()
        }
    }

    override suspend fun applyOperation(
        itemId: String,
        operation: CollaborationOperation,
    ): Either<StorageException, DocumentState> {
        return try {
            dbQuery {
                // Save operation
                CollaborationOperationsTable.insert {
                    it[id] = operation.id
                    it[CollaborationOperationsTable.itemId] = itemId
                    it[userId] = operation.userId
                    it[operationType] = when (operation) {
                        is CollaborationOperation.Insert -> "INSERT"
                        is CollaborationOperation.Delete -> "DELETE"
                        is CollaborationOperation.Retain -> "RETAIN"
                    }
                    it[position] = when (operation) {
                        is CollaborationOperation.Insert -> operation.position
                        is CollaborationOperation.Delete -> operation.position
                        is CollaborationOperation.Retain -> 0
                    }
                    it[text] = when (operation) {
                        is CollaborationOperation.Insert -> operation.text
                        else -> null
                    }
                    it[length] = when (operation) {
                        is CollaborationOperation.Delete -> operation.length
                        is CollaborationOperation.Retain -> operation.count
                        else -> null
                    }
                    it[baseVersion] = operation.baseVersion
                    it[timestamp] = operation.timestamp
                }

                // Get current state and apply operation
                val currentState = DocumentStatesTable.selectAll()
                    .where { DocumentStatesTable.itemId eq itemId }
                    .map { it.toDocumentState() }
                    .singleOrNull()

                val newContent = if (currentState != null) {
                    applyOperationToContent(currentState.content, operation)
                } else {
                    ""
                }

                val newVersion = (currentState?.version ?: 0) + 1
                val newState = DocumentState(
                    itemId = itemId,
                    version = newVersion,
                    content = newContent,
                    operations = emptyList(),
                    lastModified = Clock.System.now(),
                )

                // Save new state
                if (currentState != null) {
                    DocumentStatesTable.update({ DocumentStatesTable.itemId eq itemId }) {
                        it[version] = newVersion
                        it[content] = newContent
                        it[lastModified] = newState.lastModified
                    }
                } else {
                    DocumentStatesTable.insert {
                        it[id] = java.util.UUID.randomUUID().toString()
                        it[DocumentStatesTable.itemId] = itemId
                        it[version] = newVersion
                        it[content] = newContent
                        it[lastModified] = newState.lastModified
                    }
                }

                newState
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to apply operation: ${e.message}", e).left()
        }
    }

    override suspend fun getOperationsSince(
        itemId: String,
        sinceVersion: Long,
    ): Either<StorageException, List<CollaborationOperation>> {
        return try {
            dbQuery {
                CollaborationOperationsTable.selectAll()
                    .where {
                        (CollaborationOperationsTable.itemId eq itemId) and
                            (CollaborationOperationsTable.baseVersion greater sinceVersion)
                    }
                    .orderBy(CollaborationOperationsTable.baseVersion, SortOrder.ASC)
                    .map { it.toCollaborationOperation() }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to get operations: ${e.message}", e).left()
        }
    }

    override fun streamOperations(itemId: String): Flow<CollaborationOperation> = flow {
        // Real-time streaming would use a different mechanism
        // For now, just emit existing operations
    }

    // ========================================================================
    // Presence (simplified - in production use Redis/in-memory cache)
    // ========================================================================

    override suspend fun updatePresence(presence: UserPresence): Either<StorageException, UserPresence> {
        return try {
            dbQuery {
                val exists = UserPresenceTable.selectAll()
                    .where { UserPresenceTable.userId eq presence.userId }
                    .count() > 0

                if (exists) {
                    UserPresenceTable.update({ UserPresenceTable.userId eq presence.userId }) {
                        it[status] = presence.status.name
                        it[lastSeen] = presence.lastSeen
                        it[activeSession] = presence.activeSession
                        it[activeDocument] = presence.activeDocument
                    }
                } else {
                    UserPresenceTable.insert {
                        it[userId] = presence.userId
                        it[status] = presence.status.name
                        it[lastSeen] = presence.lastSeen
                        it[activeSession] = presence.activeSession
                        it[activeDocument] = presence.activeDocument
                    }
                }
            }
            presence.right()
        } catch (e: Exception) {
            DatabaseException("Failed to update presence: ${e.message}", e).left()
        }
    }

    override suspend fun getPresence(userId: String): Either<StorageException, UserPresence?> {
        return try {
            dbQuery {
                UserPresenceTable.selectAll()
                    .where { UserPresenceTable.userId eq userId }
                    .map { it.toUserPresence() }
                    .singleOrNull()
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to get presence: ${e.message}", e).left()
        }
    }

    override suspend fun getPresenceBulk(userIds: List<String>): Either<StorageException, Map<String, UserPresence>> {
        return try {
            dbQuery {
                UserPresenceTable.selectAll()
                    .where { UserPresenceTable.userId inList userIds }
                    .associate { it[UserPresenceTable.userId] to it.toUserPresence() }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to get presence bulk: ${e.message}", e).left()
        }
    }

    override suspend fun setOffline(userId: String): Either<StorageException, Unit> {
        return try {
            dbQuery {
                UserPresenceTable.update({ UserPresenceTable.userId eq userId }) {
                    it[status] = PresenceStatus.OFFLINE.name
                    it[lastSeen] = Clock.System.now()
                    it[activeSession] = null
                    it[activeDocument] = null
                }
            }
            Unit.right()
        } catch (e: Exception) {
            DatabaseException("Failed to set offline: ${e.message}", e).left()
        }
    }

    override suspend fun getOnlineUsersInFolder(folderId: String?): Either<StorageException, List<UserPresence>> {
        return try {
            dbQuery {
                UserPresenceTable.selectAll()
                    .where { UserPresenceTable.status eq PresenceStatus.ONLINE.name }
                    .map { it.toUserPresence() }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to get online users: ${e.message}", e).left()
        }
    }

    override fun streamPresence(userIds: List<String>): Flow<UserPresence> = flow {
        // Real-time streaming would use a different mechanism
    }

    // ========================================================================
    // Comments
    // ========================================================================

    override suspend fun createComment(comment: DocumentComment): Either<StorageException, DocumentComment> {
        return try {
            dbQuery {
                DocumentCommentsTable.insert {
                    it[id] = comment.id
                    it[itemId] = comment.itemId
                    it[userId] = comment.userId
                    it[content] = comment.content
                    it[anchorStartLine] = comment.anchor.startLine
                    it[anchorStartColumn] = comment.anchor.startColumn
                    it[anchorEndLine] = comment.anchor.endLine
                    it[anchorEndColumn] = comment.anchor.endColumn
                    it[quotedText] = comment.anchor.quotedText
                    it[createdAt] = comment.createdAt
                    it[updatedAt] = comment.updatedAt
                }
            }
            comment.right()
        } catch (e: Exception) {
            DatabaseException("Failed to create comment: ${e.message}", e).left()
        }
    }

    override suspend fun findComment(commentId: String): Either<StorageException, DocumentComment?> {
        return try {
            dbQuery {
                DocumentCommentsTable.selectAll()
                    .where { DocumentCommentsTable.id eq commentId }
                    .map { row ->
                        val replies = getRepliesForComment(commentId)
                        row.toDocumentComment(replies)
                    }
                    .singleOrNull()
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to find comment: ${e.message}", e).left()
        }
    }

    override suspend fun getCommentsForItem(
        itemId: String,
        includeResolved: Boolean,
    ): Either<StorageException, List<DocumentComment>> {
        return try {
            dbQuery {
                val query = DocumentCommentsTable.selectAll()
                    .where { DocumentCommentsTable.itemId eq itemId }

                if (!includeResolved) {
                    query.andWhere { DocumentCommentsTable.resolvedAt.isNull() }
                }

                query.orderBy(DocumentCommentsTable.createdAt, SortOrder.ASC)
                    .map { row ->
                        val replies = getRepliesForComment(row[DocumentCommentsTable.id])
                        row.toDocumentComment(replies)
                    }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to get comments: ${e.message}", e).left()
        }
    }

    override suspend fun updateComment(comment: DocumentComment): Either<StorageException, DocumentComment> {
        return try {
            dbQuery {
                DocumentCommentsTable.update({ DocumentCommentsTable.id eq comment.id }) {
                    it[content] = comment.content
                    it[updatedAt] = Clock.System.now()
                }
            }
            comment.right()
        } catch (e: Exception) {
            DatabaseException("Failed to update comment: ${e.message}", e).left()
        }
    }

    override suspend fun resolveComment(
        commentId: String,
        resolvedAt: Instant,
    ): Either<StorageException, DocumentComment> {
        return try {
            dbQuery {
                DocumentCommentsTable.update({ DocumentCommentsTable.id eq commentId }) {
                    it[DocumentCommentsTable.resolvedAt] = resolvedAt
                    it[updatedAt] = Clock.System.now()
                }
            }
            findComment(commentId).fold(
                { it.left() },
                { comment -> comment?.right() ?: DatabaseException("Comment not found").left() },
            )
        } catch (e: Exception) {
            DatabaseException("Failed to resolve comment: ${e.message}", e).left()
        }
    }

    override suspend fun addReply(
        commentId: String,
        reply: CommentReply,
    ): Either<StorageException, DocumentComment> {
        return try {
            dbQuery {
                CommentRepliesTable.insert {
                    it[id] = reply.id
                    it[CommentRepliesTable.commentId] = commentId
                    it[userId] = reply.userId
                    it[content] = reply.content
                    it[createdAt] = reply.createdAt
                }
            }
            findComment(commentId).fold(
                { it.left() },
                { comment -> comment?.right() ?: DatabaseException("Comment not found").left() },
            )
        } catch (e: Exception) {
            DatabaseException("Failed to add reply: ${e.message}", e).left()
        }
    }

    override suspend fun deleteComment(commentId: String): Either<StorageException, Unit> {
        return try {
            dbQuery {
                CommentRepliesTable.deleteWhere { CommentRepliesTable.commentId eq commentId }
                DocumentCommentsTable.deleteWhere { DocumentCommentsTable.id eq commentId }
            }
            Unit.right()
        } catch (e: Exception) {
            DatabaseException("Failed to delete comment: ${e.message}", e).left()
        }
    }

    override fun streamComments(itemId: String): Flow<DocumentComment> = flow {
        // Real-time streaming would use a different mechanism
    }

    private fun getRepliesForComment(commentId: String): List<CommentReply> {
        return CommentRepliesTable.selectAll()
            .where { CommentRepliesTable.commentId eq commentId }
            .orderBy(CommentRepliesTable.createdAt, SortOrder.ASC)
            .map { it.toCommentReply() }
    }
}
