/**
 * Collaboration Mappers
 */

package com.vaultstadio.app.data.collaboration.mapper

import com.vaultstadio.app.data.collaboration.dto.CollaborationParticipantDTO
import com.vaultstadio.app.data.collaboration.dto.CollaborationSessionDTO
import com.vaultstadio.app.data.collaboration.dto.CommentAnchorDTO
import com.vaultstadio.app.data.collaboration.dto.CommentReplyDTO
import com.vaultstadio.app.data.collaboration.dto.CursorPositionDTO
import com.vaultstadio.app.data.collaboration.dto.DocumentCommentDTO
import com.vaultstadio.app.data.collaboration.dto.DocumentStateDTO
import com.vaultstadio.app.data.collaboration.dto.TextSelectionDTO
import com.vaultstadio.app.data.collaboration.dto.UserPresenceDTO
import com.vaultstadio.app.domain.collaboration.model.CollaborationParticipant
import com.vaultstadio.app.domain.collaboration.model.CollaborationSession
import com.vaultstadio.app.domain.collaboration.model.CommentAnchor
import com.vaultstadio.app.domain.collaboration.model.CommentReply
import com.vaultstadio.app.domain.collaboration.model.CursorPosition
import com.vaultstadio.app.domain.collaboration.model.DocumentComment
import com.vaultstadio.app.domain.collaboration.model.DocumentState
import com.vaultstadio.app.domain.collaboration.model.PresenceStatus
import com.vaultstadio.app.domain.collaboration.model.TextSelection
import com.vaultstadio.app.domain.collaboration.model.UserPresence

fun CursorPositionDTO.toDomain(): CursorPosition = CursorPosition(line, column, offset)
fun TextSelectionDTO.toDomain(): TextSelection = TextSelection(start.toDomain(), end.toDomain())
fun CollaborationParticipantDTO.toDomain(): CollaborationParticipant = CollaborationParticipant(
    id,
    userId,
    userName,
    color,
    cursor?.toDomain(),
    selection?.toDomain(),
    isEditing,
)
fun CollaborationSessionDTO.toDomain(): CollaborationSession = CollaborationSession(
    id,
    itemId,
    participantId,
    participants.map { it.toDomain() },
    documentVersion,
    createdAt,
    expiresAt,
)
fun DocumentStateDTO.toDomain(): DocumentState = DocumentState(itemId, version, content, lastModified)
fun CommentAnchorDTO.toDomain(): CommentAnchor = CommentAnchor(startLine, startColumn, endLine, endColumn, quotedText)
fun CommentReplyDTO.toDomain(): CommentReply = CommentReply(id, userId, userName, content, createdAt)
fun DocumentCommentDTO.toDomain(): DocumentComment = DocumentComment(
    id,
    itemId,
    userId,
    userName,
    content,
    anchor.toDomain(),
    isResolved,
    replies.map { it.toDomain() },
    createdAt,
    updatedAt,
)
fun UserPresenceDTO.toDomain(): UserPresence = UserPresence(
    userId,
    userName,
    try {
        PresenceStatus.valueOf(status)
    } catch (e: Exception) {
        PresenceStatus.OFFLINE
    },
    lastSeen,
    activeDocument,
)
fun List<CollaborationParticipantDTO>.toParticipantList() = map { it.toDomain() }
fun List<DocumentCommentDTO>.toCommentList() = map { it.toDomain() }
fun List<UserPresenceDTO>.toPresenceList() = map { it.toDomain() }
