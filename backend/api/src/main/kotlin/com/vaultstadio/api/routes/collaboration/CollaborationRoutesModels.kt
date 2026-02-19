/**
 * DTOs and response mappers for Collaboration API.
 * Extracted from CollaborationRoutes to keep the main file under the line limit.
 */

package com.vaultstadio.api.routes.collaboration

import com.vaultstadio.core.domain.model.CollaborationParticipant
import com.vaultstadio.core.domain.model.DocumentComment
import kotlinx.serialization.Serializable

@Serializable
data class JoinSessionRequest(val itemId: String)

@Serializable
data class CollaborationSessionResponse(
    val id: String,
    val itemId: String,
    val participantId: String,
    val participants: List<ParticipantResponse>,
    val documentVersion: Long,
    val createdAt: String,
    val expiresAt: String,
)

@Serializable
data class ParticipantResponse(
    val id: String,
    val userId: String,
    val userName: String,
    val color: String,
    val cursor: CursorPositionResponse?,
    val selection: TextSelectionResponse?,
    val isEditing: Boolean,
)

@Serializable
data class CursorPositionResponse(val line: Int, val column: Int, val offset: Int)

@Serializable
data class TextSelectionResponse(
    val start: CursorPositionResponse,
    val end: CursorPositionResponse,
)

@Serializable
data class UpdateCursorRequest(val line: Int, val column: Int, val offset: Int)

@Serializable
data class UpdateSelectionRequest(
    val start: CursorPositionResponse,
    val end: CursorPositionResponse,
)

@Serializable
data class OperationRequest(
    val type: String,
    val position: Int,
    val text: String? = null,
    val length: Int? = null,
    val baseVersion: Long,
)

@Serializable
data class DocumentStateResponse(
    val itemId: String,
    val version: Long,
    val content: String,
    val lastModified: String,
)

@Serializable
data class CreateCommentRequest(
    val content: String,
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
    val quotedText: String? = null,
)

@Serializable
data class CommentResponse(
    val id: String,
    val itemId: String,
    val userId: String,
    val userName: String?,
    val content: String,
    val anchor: CommentAnchorResponse,
    val isResolved: Boolean,
    val replies: List<CommentReplyResponse>,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class CommentAnchorResponse(
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
    val quotedText: String?,
)

@Serializable
data class CommentReplyResponse(
    val id: String,
    val userId: String,
    val userName: String?,
    val content: String,
    val createdAt: String,
)

@Serializable
data class AddReplyRequest(val content: String)

@Serializable
data class UpdatePresenceRequest(
    val status: String,
    val activeDocument: String? = null,
)

@Serializable
data class UserPresenceResponse(
    val userId: String,
    val userName: String?,
    val status: String,
    val lastSeen: String,
    val activeDocument: String?,
)

internal fun CollaborationParticipant.toResponse() = ParticipantResponse(
    id = id,
    userId = userId,
    userName = userName,
    color = color,
    cursor = cursor?.let { CursorPositionResponse(it.line, it.column, it.offset) },
    selection = selection?.let {
        TextSelectionResponse(
            start = CursorPositionResponse(it.start.line, it.start.column, it.start.offset),
            end = CursorPositionResponse(it.end.line, it.end.column, it.end.offset),
        )
    },
    isEditing = isEditing,
)

internal fun DocumentComment.toResponse() = CommentResponse(
    id = id,
    itemId = itemId,
    userId = userId,
    userName = null,
    content = content,
    anchor = CommentAnchorResponse(
        startLine = anchor.startLine,
        startColumn = anchor.startColumn,
        endLine = anchor.endLine,
        endColumn = anchor.endColumn,
        quotedText = anchor.quotedText,
    ),
    isResolved = isResolved,
    replies = replies.map { reply ->
        CommentReplyResponse(
            id = reply.id,
            userId = reply.userId,
            userName = null,
            content = reply.content,
            createdAt = reply.createdAt.toString(),
        )
    },
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)
