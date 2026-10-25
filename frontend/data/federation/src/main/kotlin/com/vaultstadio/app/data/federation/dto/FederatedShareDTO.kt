/**
 * Federated share DTO.
 */

package com.vaultstadio.app.data.federation.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class FederatedShareDTO(
    val id: String,
    val itemId: String,
    val sourceInstance: String,
    val targetInstance: String,
    val targetUserId: String? = null,
    val permissions: List<String>,
    val status: String,
    @kotlinx.serialization.Contextual
    val expiresAt: Instant? = null,
    val createdBy: String,
    @kotlinx.serialization.Contextual
    val createdAt: Instant,
    @kotlinx.serialization.Contextual
    val acceptedAt: Instant? = null,
)
