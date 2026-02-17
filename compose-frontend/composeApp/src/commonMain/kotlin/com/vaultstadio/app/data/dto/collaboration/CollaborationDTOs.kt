/**
 * Collaboration Data Transfer Objects
 */

package com.vaultstadio.app.data.dto.collaboration

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CollaborationSessionDTO(
    val id: String,
    val itemId: String,
    val participantId: String,
    val participants: List<CollaborationParticipantDTO>,
    val documentVersion: Long,
    val createdAt: Instant,
    val expiresAt: Instant,
)

@Serializable
data class CollaborationParticipantDTO(
    val id: String,
    val userId: String,
    val userName: String,
    val color: String,
    val cursor: CursorPositionDTO? = null,
    val selection: TextSelectionDTO? = null,
    val isEditing: Boolean = false,
)

@Serializable
data class CursorPositionDTO(val line: Int, val column: Int, val offset: Int)

@Serializable
data class TextSelectionDTO(val start: CursorPositionDTO, val end: CursorPositionDTO)

@Serializable
data class DocumentStateDTO(val itemId: String, val version: Long, val content: String, val lastModified: Instant)

@Serializable
data class DocumentCommentDTO(
    val id: String,
    val itemId: String,
    val userId: String,
    val userName: String? = null,
    val content: String,
    val anchor: CommentAnchorDTO,
    val isResolved: Boolean,
    val replies: List<CommentReplyDTO>,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class CommentAnchorDTO(
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
    val quotedText: String? = null,
)

@Serializable
data class CommentReplyDTO(
    val id: String,
    val userId: String,
    val userName: String? = null,
    val content: String,
    val createdAt: Instant,
)

@Serializable
data class UserPresenceDTO(
    val userId: String,
    val userName: String? = null,
    val status: String,
    val lastSeen: Instant,
    val activeDocument: String? = null,
)
