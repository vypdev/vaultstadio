package com.vaultstadio.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

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
