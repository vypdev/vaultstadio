/**
 * Federation Data Transfer Objects
 */

package com.vaultstadio.app.data.dto.federation

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
    val lastSeenAt: Instant? = null,
    val registeredAt: Instant,
)

@Serializable
data class FederatedShareDTO(
    val id: String,
    val itemId: String,
    val sourceInstance: String,
    val targetInstance: String,
    val targetUserId: String? = null,
    val permissions: List<String>,
    val status: String,
    val expiresAt: Instant? = null,
    val createdBy: String,
    val createdAt: Instant,
    val acceptedAt: Instant? = null,
)

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
    val linkedAt: Instant,
)

@Serializable
data class FederatedActivityDTO(
    val id: String,
    val instanceDomain: String,
    val activityType: String,
    val actorId: String,
    val objectId: String,
    val objectType: String,
    val summary: String,
    val timestamp: Instant,
)

@Serializable
data class CreateFederatedShareRequestDTO(
    val itemId: String,
    val targetInstance: String,
    val targetUserId: String? = null,
    val permissions: List<String> = listOf("READ"),
    val expiresInDays: Int? = null,
)

@Serializable
data class LinkIdentityRequestDTO(val remoteUserId: String, val remoteInstance: String, val displayName: String)
