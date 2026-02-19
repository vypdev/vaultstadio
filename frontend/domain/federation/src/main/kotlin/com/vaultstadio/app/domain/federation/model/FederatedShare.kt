/**
 * Federated share domain model.
 */

package com.vaultstadio.app.domain.federation.model

import kotlinx.datetime.Instant

data class FederatedShare(
    val id: String,
    val itemId: String,
    val sourceInstance: String,
    val targetInstance: String,
    val targetUserId: String? = null,
    val permissions: List<SharePermission>,
    val status: FederatedShareStatus,
    val expiresAt: Instant? = null,
    val createdBy: String,
    val createdAt: Instant,
    val acceptedAt: Instant? = null,
)
