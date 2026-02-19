/**
 * Federated identity domain model.
 */

package com.vaultstadio.app.domain.federation.model

import kotlinx.datetime.Instant

data class FederatedIdentity(
    val id: String,
    val localUserId: String? = null,
    val remoteUserId: String,
    val remoteInstance: String,
    val displayName: String,
    val email: String? = null,
    val avatarUrl: String? = null,
    val verified: Boolean,
    val linkedAt: Instant,
) {
    val federatedId: String get() = "$remoteUserId@$remoteInstance"
}
