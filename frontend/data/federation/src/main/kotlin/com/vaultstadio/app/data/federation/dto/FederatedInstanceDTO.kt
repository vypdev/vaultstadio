/**
 * Federated instance DTO.
 */

package com.vaultstadio.app.data.federation.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class FederatedInstanceDTO(
    val id: String,
    val domain: String,
    val name: String,
    val description: String? = null,
    val version: String,
    val capabilities: List<String>,
    val status: String,
    @kotlinx.serialization.Contextual
    val lastSeenAt: Instant? = null,
    @kotlinx.serialization.Contextual
    val registeredAt: Instant,
)
