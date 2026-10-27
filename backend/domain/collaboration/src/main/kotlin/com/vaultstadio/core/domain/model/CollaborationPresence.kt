package com.vaultstadio.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

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
