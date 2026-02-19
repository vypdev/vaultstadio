/**
 * Federated instance domain model.
 */

package com.vaultstadio.app.domain.federation.model

import kotlinx.datetime.Instant

data class FederatedInstance(
    val id: String,
    val domain: String,
    val name: String,
    val description: String? = null,
    val version: String,
    val capabilities: List<FederationCapability>,
    val status: InstanceStatus,
    val lastSeenAt: Instant? = null,
    val registeredAt: Instant,
) {
    val isOnline: Boolean get() = status == InstanceStatus.ONLINE
}
