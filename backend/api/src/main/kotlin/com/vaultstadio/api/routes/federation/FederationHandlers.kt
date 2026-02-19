/**
 * Federation request handlers and response mappers.
 * Extracted from FederationRoutes to keep the main file under the line limit.
 */

package com.vaultstadio.api.routes.federation

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.core.domain.model.FederatedIdentity
import com.vaultstadio.core.domain.model.FederatedInstance
import com.vaultstadio.core.domain.model.FederatedShare
import com.vaultstadio.core.domain.model.FederatedShareStatus
import com.vaultstadio.core.domain.model.FederationCapability
import com.vaultstadio.core.domain.model.FederationRequest
import com.vaultstadio.core.domain.model.InstanceStatus
import com.vaultstadio.core.domain.model.SharePermission
import com.vaultstadio.core.domain.service.CreateFederatedShareInput
import com.vaultstadio.core.domain.service.FederationService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

internal suspend fun handleWellKnown(call: ApplicationCall) {
    call.respond(
        mapOf(
            "version" to "2.0.0",
            "name" to "VaultStadio Instance",
            "capabilities" to listOf(
                "RECEIVE_SHARES",
                "SEND_SHARES",
                "FEDERATED_IDENTITY",
                "ACTIVITY_STREAM",
            ),
            "endpoints" to mapOf(
                "federation" to "/api/v1/federation",
                "shares" to "/api/v1/federation/shares",
                "activities" to "/api/v1/federation/activities",
            ),
        ),
    )
}

internal suspend fun handleFederationRequest(call: ApplicationCall, federationService: FederationService) {
    val request = call.receive<IncomingFederationRequest>()
    val federationRequest = FederationRequest(
        sourceInstance = request.sourceInstance,
        sourceName = request.sourceName,
        sourceVersion = request.sourceVersion,
        publicKey = request.publicKey,
        capabilities = request.capabilities.mapNotNull { cap ->
            try {
                FederationCapability.valueOf(cap)
            } catch (e: Exception) {
                null
            }
        },
        message = request.message,
    )
    federationService.handleFederationRequest(federationRequest).fold(
        { error -> call.respond(HttpStatusCode.BadRequest, error.message ?: "Error") },
        { response ->
            call.respond(
                FederationResponseBody(
                    accepted = response.accepted,
                    instanceId = response.instanceId,
                    publicKey = response.publicKey,
                    capabilities = response.capabilities.map { it.name },
                    message = response.message,
                ),
            )
        },
    )
}

internal suspend fun handleFederationHealth(call: ApplicationCall) {
    call.respond(
        mapOf(
            "status" to "online",
            "timestamp" to Clock.System.now().toString(),
        ),
    )
}

internal suspend fun handleRequestFederation(call: ApplicationCall, federationService: FederationService) {
    val request = call.receive<FederationRequestBody>()
    federationService.requestFederation(request.targetDomain, request.message).fold(
        { error -> call.respond(HttpStatusCode.BadRequest, error.message ?: "Error") },
        { instance ->
            call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = instance.toResponse()))
        },
    )
}

internal suspend fun handleListInstances(call: ApplicationCall, federationService: FederationService) {
    val statusParam = call.request.queryParameters["status"]
    val status = statusParam?.let {
        try {
            InstanceStatus.valueOf(it)
        } catch (e: Exception) {
            null
        }
    }
    federationService.listInstances(status).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
        { instances ->
            call.respond(ApiResponse(success = true, data = instances.map { it.toResponse() }))
        },
    )
}

internal suspend fun handleGetInstance(call: ApplicationCall, federationService: FederationService) {
    val domain = call.parameters["domain"]
        ?: run {
            call.respond(HttpStatusCode.BadRequest, "Missing domain")
            return
        }
    federationService.getInstance(domain).fold(
        { error -> call.respond(HttpStatusCode.NotFound, error.message ?: "Instance not found") },
        { instance ->
            if (instance != null) {
                call.respond(ApiResponse(success = true, data = instance.toResponse()))
            } else {
                call.respond(HttpStatusCode.NotFound, "Instance not found")
            }
        },
    )
}

internal suspend fun handleBlockInstance(call: ApplicationCall, federationService: FederationService) {
    val instanceId = call.parameters["instanceId"]
        ?: run {
            call.respond(HttpStatusCode.BadRequest, "Missing instance ID")
            return
        }
    federationService.blockInstance(instanceId).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
        { call.respond(HttpStatusCode.OK, mapOf("message" to "Instance blocked")) },
    )
}

internal suspend fun handleRemoveInstance(call: ApplicationCall, federationService: FederationService) {
    val instanceId = call.parameters["instanceId"]
        ?: run {
            call.respond(HttpStatusCode.BadRequest, "Missing instance ID")
            return
        }
    federationService.removeInstance(instanceId).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
        { call.respond(HttpStatusCode.NoContent) },
    )
}

internal suspend fun handleCreateShare(call: ApplicationCall, federationService: FederationService) {
    val request = call.receive<CreateFederatedShareRequest>()
    val user = call.user ?: run {
        call.respond(HttpStatusCode.Unauthorized)
        return
    }
    val userId = user.id
    val permissions = request.permissions.mapNotNull { perm ->
        try {
            SharePermission.valueOf(perm)
        } catch (e: Exception) {
            null
        }
    }
    val input = CreateFederatedShareInput(
        itemId = request.itemId,
        targetInstance = request.targetInstance,
        targetUserId = request.targetUserId,
        permissions = permissions,
        expiresInDays = request.expiresInDays,
    )
    federationService.createShare(input, userId).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
        { share ->
            call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = share.toResponse()))
        },
    )
}

internal suspend fun handleGetOutgoingShares(call: ApplicationCall, federationService: FederationService) {
    val user = call.user ?: run {
        call.respond(HttpStatusCode.Unauthorized)
        return
    }
    federationService.getOutgoingShares(user.id).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
        { shares -> call.respond(ApiResponse(success = true, data = shares.map { it.toResponse() })) },
    )
}

internal suspend fun handleGetIncomingShares(call: ApplicationCall, federationService: FederationService) {
    val statusParam = call.request.queryParameters["status"]
    val status = statusParam?.let {
        try {
            FederatedShareStatus.valueOf(it)
        } catch (e: Exception) {
            null
        }
    }
    federationService.getIncomingShares(status).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
        { shares -> call.respond(ApiResponse(success = true, data = shares.map { it.toResponse() })) },
    )
}

internal suspend fun handleAcceptShare(call: ApplicationCall, federationService: FederationService) {
    val shareId = call.parameters["shareId"]
        ?: run {
            call.respond(HttpStatusCode.BadRequest, "Missing share ID")
            return
        }
    federationService.acceptShare(shareId).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
        { call.respond(HttpStatusCode.OK, mapOf("message" to "Share accepted")) },
    )
}

internal suspend fun handleDeclineShare(call: ApplicationCall, federationService: FederationService) {
    val shareId = call.parameters["shareId"]
        ?: run {
            call.respond(HttpStatusCode.BadRequest, "Missing share ID")
            return
        }
    federationService.declineShare(shareId).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
        { call.respond(HttpStatusCode.OK, mapOf("message" to "Share declined")) },
    )
}

internal suspend fun handleRevokeShare(call: ApplicationCall, federationService: FederationService) {
    val shareId = call.parameters["shareId"]
        ?: run {
            call.respond(HttpStatusCode.BadRequest, "Missing share ID")
            return
        }
    val user = call.user ?: run {
        call.respond(HttpStatusCode.Unauthorized)
        return
    }
    federationService.revokeShare(shareId, user.id).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
        { call.respond(HttpStatusCode.OK, mapOf("message" to "Share revoked")) },
    )
}

internal suspend fun handleLinkIdentity(call: ApplicationCall, federationService: FederationService) {
    val request = call.receive<LinkIdentityRequest>()
    val user = call.user ?: run {
        call.respond(HttpStatusCode.Unauthorized)
        return
    }
    federationService.linkIdentity(
        localUserId = user.id,
        remoteUserId = request.remoteUserId,
        remoteInstance = request.remoteInstance,
        displayName = request.displayName,
    ).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
        { identity ->
            call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = identity.toResponse()))
        },
    )
}

internal suspend fun handleGetLinkedIdentities(call: ApplicationCall, federationService: FederationService) {
    val user = call.user ?: run {
        call.respond(HttpStatusCode.Unauthorized)
        return
    }
    federationService.getLinkedIdentities(user.id).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
        { identities ->
            call.respond(ApiResponse(success = true, data = identities.map { it.toResponse() }))
        },
    )
}

internal suspend fun handleUnlinkIdentity(call: ApplicationCall, federationService: FederationService) {
    val identityId = call.parameters["identityId"]
        ?: run {
            call.respond(HttpStatusCode.BadRequest, "Missing identity ID")
            return
        }
    federationService.unlinkIdentity(identityId).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
        { call.respond(HttpStatusCode.NoContent) },
    )
}

internal suspend fun handleGetActivities(call: ApplicationCall, federationService: FederationService) {
    val instanceDomain = call.request.queryParameters["instance"]
    val sinceStr = call.request.queryParameters["since"]
    val since = sinceStr?.let {
        try {
            Instant.parse(it)
        } catch (e: Exception) {
            null
        }
    }
    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
    federationService.getActivities(instanceDomain, since, limit).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
        { activities ->
            call.respond(
                ApiResponse(
                    success = true,
                    data = activities.map { activity ->
                        FederatedActivityResponse(
                            id = activity.id,
                            instanceDomain = activity.instanceDomain,
                            activityType = activity.activityType.name,
                            actorId = activity.actorId,
                            objectId = activity.objectId,
                            objectType = activity.objectType,
                            summary = activity.summary,
                            timestamp = activity.timestamp.toString(),
                        )
                    },
                ),
            )
        },
    )
}

internal suspend fun handleRunHealthChecks(call: ApplicationCall, federationService: FederationService) {
    federationService.runHealthChecks().fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
        { results ->
            call.respond(
                mapOf(
                    "checked" to results.size,
                    "online" to results.count { it.value },
                    "offline" to results.count { !it.value },
                ),
            )
        },
    )
}

internal suspend fun handleCleanup(call: ApplicationCall, federationService: FederationService) {
    val olderThanDays = call.request.queryParameters["olderThanDays"]?.toIntOrNull() ?: 30
    federationService.cleanup(olderThanDays).fold(
        { error -> call.respond(HttpStatusCode.InternalServerError, error.message ?: "Error") },
        { cleanedCount ->
            call.respond(mapOf("cleaned" to cleanedCount))
        },
    )
}

internal fun FederatedInstance.toResponse() = FederatedInstanceResponse(
    id = id,
    domain = domain,
    name = name,
    description = description,
    version = version,
    capabilities = capabilities.map { it.name },
    status = status.name,
    lastSeenAt = lastSeenAt?.toString(),
    registeredAt = registeredAt.toString(),
)

internal fun FederatedShare.toResponse() = FederatedShareResponse(
    id = id,
    itemId = itemId,
    sourceInstance = sourceInstance,
    targetInstance = targetInstance,
    targetUserId = targetUserId,
    permissions = permissions.map { it.name },
    status = status.name,
    expiresAt = expiresAt?.toString(),
    createdBy = createdBy,
    createdAt = createdAt.toString(),
    acceptedAt = acceptedAt?.toString(),
)

internal fun FederatedIdentity.toResponse() = FederatedIdentityResponse(
    id = id,
    localUserId = localUserId,
    remoteUserId = remoteUserId,
    remoteInstance = remoteInstance,
    displayName = displayName,
    email = email,
    avatarUrl = avatarUrl,
    verified = verified,
    linkedAt = linkedAt.toString(),
)
