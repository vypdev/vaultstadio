/**
 * Federated identity DTO.
 */

package com.vaultstadio.app.data.federation.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class FederatedIdentityDTO(
    val id: String,
    val localUserId: String? = null,
    val remoteUserId: String,
    val remoteInstance: String,
    val displayName: String,
    val email: String? = null,
    val avatarUrl: String? = null,
    val verified: Boolean,
    @kotlinx.serialization.Contextual
    val linkedAt: Instant,
)
