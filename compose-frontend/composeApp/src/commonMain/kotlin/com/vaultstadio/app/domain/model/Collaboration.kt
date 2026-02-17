/**
 * Collaboration Domain Models
 */

package com.vaultstadio.app.domain.model

import kotlinx.datetime.Instant

data class CollaborationSession(
    val id: String,
    val itemId: String,
    val participantId: String,
    val participants: List<CollaborationParticipant>,
    val documentVersion: Long,
    val createdAt: Instant,
    val expiresAt: Instant,
)

data class CollaborationParticipant(
    val id: String,
    val userId: String,
    val userName: String,
    val color: String,
    val cursor: CursorPosition? = null,
    val selection: TextSelection? = null,
    val isEditing: Boolean = false,
)

data class CursorPosition(val line: Int, val column: Int, val offset: Int)

data class TextSelection(val start: CursorPosition, val end: CursorPosition) {
    val length: Int get() = kotlin.math.abs(end.offset - start.offset)
    val isEmpty: Boolean get() = length == 0
}

data class DocumentState(val itemId: String, val version: Long, val content: String, val lastModified: Instant)

data class DocumentComment(
    val id: String,
    val itemId: String,
    val userId: String,
    val userName: String? = null,
    val content: String,
    val anchor: CommentAnchor,
    val isResolved: Boolean,
    val replies: List<CommentReply>,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CommentAnchor(
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
    val quotedText: String? = null,
)

data class CommentReply(
    val id: String,
    val userId: String,
    val userName: String? = null,
    val content: String,
    val createdAt: Instant,
)

enum class PresenceStatus { ONLINE, AWAY, BUSY, OFFLINE }

data class UserPresence(
    val userId: String,
    val userName: String? = null,
    val status: PresenceStatus,
    val lastSeen: Instant,
    val activeDocument: String? = null,
)
