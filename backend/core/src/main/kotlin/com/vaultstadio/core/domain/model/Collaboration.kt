/**
 * VaultStadio Real-time Collaboration Models
 *
 * Models for real-time collaboration features including
 * presence, cursors, and operational transformation.
 */

package com.vaultstadio.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Represents a collaboration session for a document.
 *
 * @property id Unique session identifier
 * @property itemId ID of the document being collaborated on
 * @property createdAt When the session was created
 * @property expiresAt When the session expires
 * @property participants List of current participants
 */
@Serializable
data class CollaborationSession(
    val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    val participants: List<CollaborationParticipant> = emptyList(),
) {
    val participantCount: Int get() = participants.size
    val isActive: Boolean get() = participants.isNotEmpty()
}

/**
 * Represents a participant in a collaboration session.
 *
 * @property id Unique participant identifier
 * @property userId User ID
 * @property userName Display name
 * @property color Assigned color (hex)
 * @property cursor Current cursor position
 * @property selection Current text selection
 * @property joinedAt When the participant joined
 * @property lastActiveAt Last activity timestamp
 * @property isEditing Whether currently editing
 */
@Serializable
data class CollaborationParticipant(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val userName: String,
    val color: String,
    val cursor: CursorPosition? = null,
    val selection: TextSelection? = null,
    val joinedAt: Instant,
    val lastActiveAt: Instant,
    val isEditing: Boolean = false,
)

/**
 * Cursor position in a document.
 *
 * @property line Line number (0-based)
 * @property column Column number (0-based)
 * @property offset Absolute character offset
 */
@Serializable
data class CursorPosition(
    val line: Int,
    val column: Int,
    val offset: Int,
)

/**
 * Text selection range.
 *
 * @property start Start position
 * @property end End position
 * @property isBackwards Whether selection was made backwards
 */
@Serializable
data class TextSelection(
    val start: CursorPosition,
    val end: CursorPosition,
    val isBackwards: Boolean = false,
) {
    val length: Int get() = kotlin.math.abs(end.offset - start.offset)
    val isEmpty: Boolean get() = length == 0
}

/**
 * An operation in the operational transformation system.
 * These operations can be composed and transformed for conflict resolution.
 */
@Serializable
sealed class CollaborationOperation {
    abstract val id: String
    abstract val userId: String
    abstract val timestamp: Instant
    abstract val baseVersion: Long

    /**
     * Insert text at a position.
     */
    @Serializable
    data class Insert(
        override val id: String = UUID.randomUUID().toString(),
        override val userId: String,
        override val timestamp: Instant,
        override val baseVersion: Long,
        val position: Int,
        val text: String,
    ) : CollaborationOperation()

    /**
     * Delete text at a range.
     */
    @Serializable
    data class Delete(
        override val id: String = UUID.randomUUID().toString(),
        override val userId: String,
        override val timestamp: Instant,
        override val baseVersion: Long,
        val position: Int,
        val length: Int,
        val deletedText: String? = null,
    ) : CollaborationOperation()

    /**
     * Retain/skip characters (no-op).
     */
    @Serializable
    data class Retain(
        override val id: String = UUID.randomUUID().toString(),
        override val userId: String,
        override val timestamp: Instant,
        override val baseVersion: Long,
        val count: Int,
    ) : CollaborationOperation()
}

/**
 * Document state after applying operations.
 *
 * @property itemId Storage item ID
 * @property version Current document version
 * @property content Current document content
 * @property operations List of operations applied
 * @property lastModified Last modification timestamp
 */
@Serializable
data class DocumentState(
    val itemId: String,
    val version: Long,
    val content: String,
    val operations: List<CollaborationOperation> = emptyList(),
    val lastModified: Instant,
)

/**
 * Real-time event for collaboration.
 */
@Serializable
sealed class CollaborationEvent {
    abstract val sessionId: String
    abstract val timestamp: Instant

    /**
     * Participant joined the session.
     */
    @Serializable
    data class ParticipantJoined(
        override val sessionId: String,
        override val timestamp: Instant,
        val participant: CollaborationParticipant,
    ) : CollaborationEvent()

    /**
     * Participant left the session.
     */
    @Serializable
    data class ParticipantLeft(
        override val sessionId: String,
        override val timestamp: Instant,
        val participantId: String,
    ) : CollaborationEvent()

    /**
     * Cursor position updated.
     */
    @Serializable
    data class CursorMoved(
        override val sessionId: String,
        override val timestamp: Instant,
        val participantId: String,
        val cursor: CursorPosition,
    ) : CollaborationEvent()

    /**
     * Selection changed.
     */
    @Serializable
    data class SelectionChanged(
        override val sessionId: String,
        override val timestamp: Instant,
        val participantId: String,
        val selection: TextSelection?,
    ) : CollaborationEvent()

    /**
     * Operation applied to document.
     */
    @Serializable
    data class OperationApplied(
        override val sessionId: String,
        override val timestamp: Instant,
        val operation: CollaborationOperation,
        val newVersion: Long,
    ) : CollaborationEvent()

    /**
     * Document saved.
     */
    @Serializable
    data class DocumentSaved(
        override val sessionId: String,
        override val timestamp: Instant,
        val version: Long,
        val savedBy: String,
    ) : CollaborationEvent()
}

/**
 * Presence status for a user.
 */
@Serializable
enum class PresenceStatus {
    ONLINE,
    AWAY,
    BUSY,
    OFFLINE,
}

/**
 * User presence information.
 *
 * @property userId User ID
 * @property status Current status
 * @property lastSeen Last seen timestamp
 * @property activeSession Currently active session (if any)
 * @property activeDocument Document currently being edited
 */
@Serializable
data class UserPresence(
    val userId: String,
    val status: PresenceStatus,
    val lastSeen: Instant,
    val activeSession: String? = null,
    val activeDocument: String? = null,
)

/**
 * Comment on a document section.
 *
 * @property id Unique identifier
 * @property itemId Storage item ID
 * @property userId User who created the comment
 * @property content Comment text
 * @property anchor Position anchor in the document
 * @property resolvedAt When the comment was resolved
 * @property replies Threaded replies
 * @property createdAt When the comment was created
 * @property updatedAt When the comment was last updated
 */
@Serializable
data class DocumentComment(
    val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val userId: String,
    val content: String,
    val anchor: CommentAnchor,
    val resolvedAt: Instant? = null,
    val replies: List<CommentReply> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val isResolved: Boolean get() = resolvedAt != null
}

/**
 * Anchor position for a comment.
 *
 * @property startLine Starting line
 * @property startColumn Starting column
 * @property endLine Ending line
 * @property endColumn Ending column
 * @property quotedText The text being commented on
 */
@Serializable
data class CommentAnchor(
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
    val quotedText: String? = null,
)

/**
 * Reply to a comment.
 *
 * @property id Unique identifier
 * @property userId User who created the reply
 * @property content Reply text
 * @property createdAt When the reply was created
 */
@Serializable
data class CommentReply(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val content: String,
    val createdAt: Instant,
)
