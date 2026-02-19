/**
 * Collaboration Participant
 */

package com.vaultstadio.app.domain.collaboration.model

data class CollaborationParticipant(
    val id: String,
    val userId: String,
    val userName: String,
    val color: String,
    val cursor: CursorPosition? = null,
    val selection: TextSelection? = null,
    val isEditing: Boolean = false,
)
