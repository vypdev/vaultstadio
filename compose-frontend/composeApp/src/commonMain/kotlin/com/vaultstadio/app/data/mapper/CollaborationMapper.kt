/**
 * Collaboration Mappers
 */

package com.vaultstadio.app.data.mapper

import com.vaultstadio.app.data.dto.collaboration.CollaborationParticipantDTO
import com.vaultstadio.app.data.dto.collaboration.CollaborationSessionDTO
import com.vaultstadio.app.data.dto.collaboration.CommentAnchorDTO
import com.vaultstadio.app.data.dto.collaboration.CommentReplyDTO
import com.vaultstadio.app.data.dto.collaboration.CursorPositionDTO
import com.vaultstadio.app.data.dto.collaboration.DocumentCommentDTO
import com.vaultstadio.app.data.dto.collaboration.DocumentStateDTO
import com.vaultstadio.app.data.dto.collaboration.TextSelectionDTO
import com.vaultstadio.app.data.dto.collaboration.UserPresenceDTO
import com.vaultstadio.app.domain.model.CollaborationParticipant
import com.vaultstadio.app.domain.model.CollaborationSession
import com.vaultstadio.app.domain.model.CommentAnchor
import com.vaultstadio.app.domain.model.CommentReply
import com.vaultstadio.app.domain.model.CursorPosition
import com.vaultstadio.app.domain.model.DocumentComment
import com.vaultstadio.app.domain.model.DocumentState
import com.vaultstadio.app.domain.model.PresenceStatus
import com.vaultstadio.app.domain.model.TextSelection
import com.vaultstadio.app.domain.model.UserPresence

fun CursorPositionDTO.toDomain() = CursorPosition(line, column, offset)
fun TextSelectionDTO.toDomain() = TextSelection(start.toDomain(), end.toDomain())
fun CollaborationParticipantDTO.toDomain() = CollaborationParticipant(
    id,
    userId,
    userName,
    color,
    cursor?.toDomain(),
    selection?.toDomain(),
    isEditing,
)
fun CollaborationSessionDTO.toDomain() = CollaborationSession(
    id,
    itemId,
    participantId,
    participants.map { it.toDomain() },
    documentVersion,
    createdAt,
    expiresAt,
)
fun DocumentStateDTO.toDomain() = DocumentState(itemId, version, content, lastModified)
fun CommentAnchorDTO.toDomain() = CommentAnchor(startLine, startColumn, endLine, endColumn, quotedText)
fun CommentReplyDTO.toDomain() = CommentReply(id, userId, userName, content, createdAt)
fun DocumentCommentDTO.toDomain() = DocumentComment(
    id, itemId, userId, userName, content, anchor.toDomain(), isResolved,
    replies.map {
        it.toDomain()
    },
    createdAt, updatedAt,
)
fun UserPresenceDTO.toDomain() = UserPresence(
    userId,
    userName,
    try {
        PresenceStatus.valueOf(status)
    } catch (
        e: Exception,
    ) {
        PresenceStatus.OFFLINE
    },
    lastSeen,
    activeDocument,
)
fun List<CollaborationParticipantDTO>.toParticipantList() = map { it.toDomain() }
fun List<DocumentCommentDTO>.toCommentList() = map { it.toDomain() }
fun List<UserPresenceDTO>.toPresenceList() = map { it.toDomain() }
