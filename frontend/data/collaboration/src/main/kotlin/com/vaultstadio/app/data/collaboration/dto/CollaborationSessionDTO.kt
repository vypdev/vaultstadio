/**
 * Collaboration Session DTO
 */

package com.vaultstadio.app.data.collaboration.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CollaborationSessionDTO(
    val id: String,
    val itemId: String,
    val participantId: String,
    val participants: List<CollaborationParticipantDTO>,
    val documentVersion: Long,
    @kotlinx.serialization.Contextual val createdAt: Instant,
    @kotlinx.serialization.Contextual val expiresAt: Instant,
)
