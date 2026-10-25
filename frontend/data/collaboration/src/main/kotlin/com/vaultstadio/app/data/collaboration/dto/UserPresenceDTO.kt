/**
 * User Presence DTO
 */

package com.vaultstadio.app.data.collaboration.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UserPresenceDTO(
    val userId: String,
    val userName: String? = null,
    val status: String,
    @kotlinx.serialization.Contextual val lastSeen: Instant,
    val activeDocument: String? = null,
)
