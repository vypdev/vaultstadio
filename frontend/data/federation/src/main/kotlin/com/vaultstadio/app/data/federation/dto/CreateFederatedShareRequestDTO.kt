/**
 * Create federated share request DTO.
 */

package com.vaultstadio.app.data.federation.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateFederatedShareRequestDTO(
    val itemId: String,
    val targetInstance: String,
    val targetUserId: String? = null,
    val permissions: List<String> = listOf("READ"),
    val expiresInDays: Int? = null,
)
