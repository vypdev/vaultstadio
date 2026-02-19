/**
 * Federated activity domain model.
 */

package com.vaultstadio.app.domain.federation.model

import kotlinx.datetime.Instant

data class FederatedActivity(
    val id: String,
    val instanceDomain: String,
    val activityType: FederatedActivityType,
    val actorId: String,
    val objectId: String,
    val objectType: String,
    val summary: String,
    val timestamp: Instant,
)
