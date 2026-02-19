/**
 * User Presence
 */

package com.vaultstadio.app.domain.collaboration.model

import kotlinx.datetime.Instant

data class UserPresence(
    val userId: String,
    val userName: String? = null,
    val status: PresenceStatus,
    val lastSeen: Instant,
    val activeDocument: String? = null,
)
