/**
 * Collaboration Session
 */

package com.vaultstadio.app.domain.collaboration.model

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
