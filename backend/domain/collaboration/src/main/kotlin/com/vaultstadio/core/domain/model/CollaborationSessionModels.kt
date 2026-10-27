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
