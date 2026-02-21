/**
 * Federated activity DTO.
 */

package com.vaultstadio.app.data.federation.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class FederatedActivityDTO(
    val id: String,
    val instanceDomain: String,
    val activityType: String,
    val actorId: String,
    val objectId: String,
    val objectType: String,
    val summary: String,
    @kotlinx.serialization.Contextual
    val timestamp: Instant,
)
