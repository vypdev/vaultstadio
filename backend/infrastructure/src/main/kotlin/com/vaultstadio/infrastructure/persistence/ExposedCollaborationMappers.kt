/**
 * ResultRow mappers for collaboration entities.
 * Extracted from ExposedCollaborationRepository to keep the main file under the line limit.
 */

package com.vaultstadio.infrastructure.persistence

import com.vaultstadio.core.domain.model.CollaborationOperation
import com.vaultstadio.core.domain.model.CollaborationParticipant
import com.vaultstadio.core.domain.model.CollaborationSession
import com.vaultstadio.core.domain.model.CommentAnchor
import com.vaultstadio.core.domain.model.CommentReply
import com.vaultstadio.core.domain.model.CursorPosition
import com.vaultstadio.core.domain.model.DocumentComment
import com.vaultstadio.core.domain.model.DocumentState
import com.vaultstadio.core.domain.model.PresenceStatus
import com.vaultstadio.core.domain.model.TextSelection
import com.vaultstadio.core.domain.model.UserPresence
import com.vaultstadio.infrastructure.persistence.entities.CollaborationOperationsTable
import com.vaultstadio.infrastructure.persistence.entities.CollaborationParticipantsTable
import com.vaultstadio.infrastructure.persistence.entities.CollaborationSessionsTable
import com.vaultstadio.infrastructure.persistence.entities.CommentRepliesTable
import com.vaultstadio.infrastructure.persistence.entities.DocumentCommentsTable
import com.vaultstadio.infrastructure.persistence.entities.DocumentStatesTable
import com.vaultstadio.infrastructure.persistence.entities.UserPresenceTable
import org.jetbrains.exposed.sql.ResultRow

internal fun ResultRow.toCollaborationSession(participants: List<CollaborationParticipant>): CollaborationSession =
    CollaborationSession(
        id = this[CollaborationSessionsTable.id],
        itemId = this[CollaborationSessionsTable.itemId],
        createdAt = this[CollaborationSessionsTable.createdAt],
        expiresAt = this[CollaborationSessionsTable.expiresAt],
        participants = participants,
    )

internal fun ResultRow.toCollaborationParticipant(): CollaborationParticipant {
    val cursor = if (this[CollaborationParticipantsTable.cursorLine] != null) {
        CursorPosition(
            line = this[CollaborationParticipantsTable.cursorLine]!!,
            column = this[CollaborationParticipantsTable.cursorColumn] ?: 0,
            offset = this[CollaborationParticipantsTable.cursorOffset] ?: 0,
        )
    } else {
        null
    }

    val selection = if (this[CollaborationParticipantsTable.selectionStartLine] != null &&
        this[CollaborationParticipantsTable.selectionEndLine] != null
    ) {
        TextSelection(
            start = CursorPosition(
                line = this[CollaborationParticipantsTable.selectionStartLine]!!,
                column = this[CollaborationParticipantsTable.selectionStartColumn] ?: 0,
                offset = this[CollaborationParticipantsTable.selectionStartOffset] ?: 0,
            ),
            end = CursorPosition(
                line = this[CollaborationParticipantsTable.selectionEndLine]!!,
                column = this[CollaborationParticipantsTable.selectionEndColumn] ?: 0,
                offset = this[CollaborationParticipantsTable.selectionEndOffset] ?: 0,
            ),
        )
    } else {
        null
    }

    return CollaborationParticipant(
        id = this[CollaborationParticipantsTable.id],
        userId = this[CollaborationParticipantsTable.userId],
        userName = this[CollaborationParticipantsTable.userName],
        color = this[CollaborationParticipantsTable.color],
        cursor = cursor,
        selection = selection,
        joinedAt = this[CollaborationParticipantsTable.joinedAt],
        lastActiveAt = this[CollaborationParticipantsTable.lastActiveAt],
        isEditing = this[CollaborationParticipantsTable.isEditing],
    )
}

internal fun ResultRow.toDocumentState(): DocumentState = DocumentState(
    itemId = this[DocumentStatesTable.itemId],
    version = this[DocumentStatesTable.version],
    content = this[DocumentStatesTable.content],
    operations = emptyList(),
    lastModified = this[DocumentStatesTable.lastModified],
)

internal fun ResultRow.toCollaborationOperation(): CollaborationOperation {
    val opType = this[CollaborationOperationsTable.operationType]
    val id = this[CollaborationOperationsTable.id]
    val userId = this[CollaborationOperationsTable.userId]
    val timestamp = this[CollaborationOperationsTable.timestamp]
    val baseVersion = this[CollaborationOperationsTable.baseVersion]
    val position = this[CollaborationOperationsTable.position]

    return when (opType) {
        "INSERT" -> CollaborationOperation.Insert(
            id = id,
            userId = userId,
            timestamp = timestamp,
            baseVersion = baseVersion,
            position = position,
            text = this[CollaborationOperationsTable.text] ?: "",
        )
        "DELETE" -> CollaborationOperation.Delete(
            id = id,
            userId = userId,
            timestamp = timestamp,
            baseVersion = baseVersion,
            position = position,
            length = this[CollaborationOperationsTable.length] ?: 0,
        )
        else -> CollaborationOperation.Retain(
            id = id,
            userId = userId,
            timestamp = timestamp,
            baseVersion = baseVersion,
            count = this[CollaborationOperationsTable.length] ?: 0,
        )
    }
}

internal fun ResultRow.toUserPresence(): UserPresence = UserPresence(
    userId = this[UserPresenceTable.userId],
    status = PresenceStatus.valueOf(this[UserPresenceTable.status]),
    lastSeen = this[UserPresenceTable.lastSeen],
    activeSession = this[UserPresenceTable.activeSession],
    activeDocument = this[UserPresenceTable.activeDocument],
)

internal fun ResultRow.toDocumentComment(replies: List<CommentReply>): DocumentComment = DocumentComment(
    id = this[DocumentCommentsTable.id],
    itemId = this[DocumentCommentsTable.itemId],
    userId = this[DocumentCommentsTable.userId],
    content = this[DocumentCommentsTable.content],
    anchor = CommentAnchor(
        startLine = this[DocumentCommentsTable.anchorStartLine],
        startColumn = this[DocumentCommentsTable.anchorStartColumn],
        endLine = this[DocumentCommentsTable.anchorEndLine],
        endColumn = this[DocumentCommentsTable.anchorEndColumn],
        quotedText = this[DocumentCommentsTable.quotedText],
    ),
    resolvedAt = this[DocumentCommentsTable.resolvedAt],
    replies = replies,
    createdAt = this[DocumentCommentsTable.createdAt],
    updatedAt = this[DocumentCommentsTable.updatedAt],
)

internal fun ResultRow.toCommentReply(): CommentReply = CommentReply(
    id = this[CommentRepliesTable.id],
    userId = this[CommentRepliesTable.userId],
    content = this[CommentRepliesTable.content],
    createdAt = this[CommentRepliesTable.createdAt],
)
