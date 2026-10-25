/**
 * VaultStadio Federation Routes
 *
 * API endpoints for federation operations between VaultStadio instances.
 */

package com.vaultstadio.api.routes.federation

import com.vaultstadio.core.domain.service.FederationService
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

/**
 * Request to initiate federation.
 */
@Serializable
data class FederationRequestBody(
    val targetDomain: String,
    val message: String? = null,
)

/**
 * Federated instance response.
 */
@Serializable
data class FederatedInstanceResponse(
    val id: String,
    val domain: String,
    val name: String,
    val description: String?,
    val version: String,
    val capabilities: List<String>,
    val status: String,
    val lastSeenAt: String?,
    val registeredAt: String,
)

/**
 * Request to create a federated share.
 */
@Serializable
data class CreateFederatedShareRequest(
    val itemId: String,
    val targetInstance: String,
    val targetUserId: String? = null,
    val permissions: List<String> = listOf("READ"),
    val expiresInDays: Int? = null,
)

/**
 * Federated share response.
 */
@Serializable
data class FederatedShareResponse(
    val id: String,
    val itemId: String,
    val sourceInstance: String,
    val targetInstance: String,
    val targetUserId: String?,
    val permissions: List<String>,
    val status: String,
    val expiresAt: String?,
    val createdBy: String,
    val createdAt: String,
    val acceptedAt: String?,
)

/**
 * Federated identity response.
 */
@Serializable
data class FederatedIdentityResponse(
    val id: String,
    val localUserId: String?,
    val remoteUserId: String,
    val remoteInstance: String,
    val displayName: String,
    val email: String?,
    val avatarUrl: String?,
    val verified: Boolean,
    val linkedAt: String,
)

/**
 * Request to link an identity.
 */
@Serializable
data class LinkIdentityRequest(
    val remoteUserId: String,
    val remoteInstance: String,
    val displayName: String,
)

/**
 * Federated activity response.
 */
@Serializable
data class FederatedActivityResponse(
    val id: String,
    val instanceDomain: String,
    val activityType: String,
    val actorId: String,
    val objectId: String,
    val objectType: String,
    val summary: String,
    val timestamp: String,
)

/**
 * Incoming federation request (from another instance).
 */
@Serializable
data class IncomingFederationRequest(
    val sourceInstance: String,
    val sourceName: String,
    val sourceVersion: String,
    val publicKey: String,
    val capabilities: List<String>,
    val message: String?,
)

/**
 * Federation response.
 */
@Serializable
data class FederationResponseBody(
    val accepted: Boolean,
    val instanceId: String?,
    val publicKey: String?,
    val capabilities: List<String>,
    val message: String?,
)

/**
 * Configure federation routes.
 */
fun Route.federationRoutes(federationService: FederationService) {
    route("/api/v1/federation") {
        get("/.well-known/vaultstadio") { handleWellKnown(call) }
        post("/request") { handleFederationRequest(call, federationService) }
        get("/health") { handleFederationHealth(call) }
    }

    authenticate("auth-bearer") {
        route("/api/v1/federation") {
            route("/instances") {
                post("/request") { handleRequestFederation(call, federationService) }
                get { handleListInstances(call, federationService) }
                get("/{domain}") { handleGetInstance(call, federationService) }
                post("/{instanceId}/block") { handleBlockInstance(call, federationService) }
                delete("/{instanceId}") { handleRemoveInstance(call, federationService) }
            }

            route("/shares") {
                post { handleCreateShare(call, federationService) }
                get("/outgoing") { handleGetOutgoingShares(call, federationService) }
                get("/incoming") { handleGetIncomingShares(call, federationService) }
                post("/{shareId}/accept") { handleAcceptShare(call, federationService) }
                post("/{shareId}/decline") { handleDeclineShare(call, federationService) }
                post("/{shareId}/revoke") { handleRevokeShare(call, federationService) }
            }

            route("/identities") {
                post { handleLinkIdentity(call, federationService) }
                get { handleGetLinkedIdentities(call, federationService) }
                delete("/{identityId}") { handleUnlinkIdentity(call, federationService) }
            }

            route("/activities") {
                get { handleGetActivities(call, federationService) }
            }

            route("/admin") {
                post("/health-check") { handleRunHealthChecks(call, federationService) }
                post("/cleanup") { handleCleanup(call, federationService) }
            }
        }
    }
}
