/**
 * Collaboration Participant DTO
 */

package com.vaultstadio.app.data.collaboration.dto

import kotlinx.serialization.Serializable

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
