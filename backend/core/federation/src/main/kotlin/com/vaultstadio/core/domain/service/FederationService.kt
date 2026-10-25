/**
 * VaultStadio Federation Service
 *
 * Business logic for federation operations.
 */

package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.FederatedActivity
import com.vaultstadio.core.domain.model.FederatedActivityType
import com.vaultstadio.core.domain.model.FederatedIdentity
import com.vaultstadio.core.domain.model.FederatedInstance
import com.vaultstadio.core.domain.model.FederatedShare
import com.vaultstadio.core.domain.model.FederatedShareStatus
import com.vaultstadio.core.domain.model.FederationCapability
import com.vaultstadio.core.domain.model.FederationRequest
import com.vaultstadio.core.domain.model.FederationResponse
import com.vaultstadio.core.domain.model.InstanceStatus
import com.vaultstadio.core.domain.model.SharePermission
import com.vaultstadio.core.domain.model.SignedFederationMessage
import com.vaultstadio.core.domain.repository.FederationRepository
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.InvalidOperationException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.domain.common.exception.StorageException
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.days

private val logger = KotlinLogging.logger {}

/**
 * Input for creating a federated share.
 *
 * @property itemId Storage item ID
 * @property targetInstance Target instance domain
 * @property targetUserId Target user ID (optional)
 * @property permissions Granted permissions
 * @property expiresInDays Days until expiration (null for no expiration)
 */
data class CreateFederatedShareInput(
    val itemId: String,
    val targetInstance: String,
    val targetUserId: String? = null,
    val permissions: List<SharePermission> = listOf(SharePermission.READ),
    val expiresInDays: Int? = null,
)

/**
 * Configuration for this VaultStadio instance.
 *
 * @property domain This instance's domain
 * @property name This instance's name
 * @property version VaultStadio version
 * @property publicKey This instance's public key
 * @property privateKey This instance's private key (for signing)
 * @property capabilities Supported capabilities
 */
data class InstanceConfig(
    val domain: String,
    val name: String,
    val version: String,
    val publicKey: String,
    val privateKey: String,
    val capabilities: List<FederationCapability>,
)

/**
 * Service for managing federation between VaultStadio instances.
 *
 * @property federationRepository Repository for federation persistence
 * @property instanceConfig Configuration for this instance
 */
class FederationService(
    private val federationRepository: FederationRepository,
    private val instanceConfig: InstanceConfig,
) {

    // ========================================================================
    // Instance Discovery & Management
    // ========================================================================

    /**
     * Request federation with another instance.
     *
     * @param targetDomain Target instance domain
     * @param message Optional message to administrator
     * @return The registered instance (pending status)
     */
    suspend fun requestFederation(
        targetDomain: String,
        message: String? = null,
    ): Either<StorageException, FederatedInstance> {
        // Check if already federated
        return federationRepository.findInstanceByDomain(targetDomain).flatMap { existing ->
            if (existing != null) {
                InvalidOperationException(
                    operation = "requestFederation",
                    message = "Already federated with $targetDomain",
                ).left()
            } else {
                // In production, this would send an HTTP request to the target instance
                val request = FederationRequest(
                    sourceInstance = instanceConfig.domain,
                    sourceName = instanceConfig.name,
                    sourceVersion = instanceConfig.version,
                    publicKey = instanceConfig.publicKey,
                    capabilities = instanceConfig.capabilities,
                    message = message,
                )

                // Create pending instance record
                val instance = FederatedInstance(
                    id = UUID.randomUUID().toString(),
                    domain = targetDomain,
                    name = targetDomain, // Will be updated when accepted
                    version = "unknown",
                    publicKey = "", // Will be populated when accepted
                    status = InstanceStatus.PENDING,
                    registeredAt = Clock.System.now(),
                )

                federationRepository.registerInstance(instance)
            }
        }
    }

    /**
     * Handle an incoming federation request.
     *
     * @param request The federation request
     * @return Response to the request
     */
    suspend fun handleFederationRequest(
        request: FederationRequest,
    ): Either<StorageException, FederationResponse> {
        // Check if already federated
        return federationRepository.findInstanceByDomain(request.sourceInstance).flatMap { existing ->
            if (existing != null) {
                FederationResponse(
                    accepted = false,
                    message = "Already federated",
                ).right()
            } else {
                // Auto-accept for now (in production, admin approval would be required)
                val instance = FederatedInstance(
                    id = UUID.randomUUID().toString(),
                    domain = request.sourceInstance,
                    name = request.sourceName,
                    version = request.sourceVersion,
                    publicKey = request.publicKey,
                    capabilities = request.capabilities,
                    status = InstanceStatus.ONLINE,
                    lastSeenAt = Clock.System.now(),
                    registeredAt = Clock.System.now(),
                )

                federationRepository.registerInstance(instance).map {
                    FederationResponse(
                        accepted = true,
                        instanceId = it.id,
                        publicKey = instanceConfig.publicKey,
                        capabilities = instanceConfig.capabilities,
                        message = "Federation accepted",
                    )
                }
            }
        }
    }

    /**
     * List all federated instances.
     *
     * @param status Filter by status
     * @return List of instances
     */
    suspend fun listInstances(
        status: InstanceStatus? = null,
    ): Either<StorageException, List<FederatedInstance>> {
        return federationRepository.listInstances(status)
    }

    /**
     * Get an instance by domain.
     *
     * @param domain Instance domain
     * @return The instance
     */
    suspend fun getInstance(domain: String): Either<StorageException, FederatedInstance> {
        return federationRepository.findInstanceByDomain(domain).flatMap { instance ->
            instance?.right() ?: ItemNotFoundException(
                message = "Instance not found: $domain",
            ).left()
        }
    }

    /**
     * Block an instance.
     *
     * @param instanceId Instance ID
     * @return The updated instance
     */
    suspend fun blockInstance(instanceId: String): Either<StorageException, FederatedInstance> {
        return federationRepository.updateInstanceStatus(
            instanceId,
            InstanceStatus.BLOCKED,
        )
    }

    /**
     * Remove an instance.
     *
     * @param instanceId Instance ID
     * @return Unit on success
     */
    suspend fun removeInstance(instanceId: String): Either<StorageException, Unit> {
        return federationRepository.removeInstance(instanceId)
    }

    /**
     * Update instance status after a health check.
     *
     * @param domain Instance domain
     * @param isOnline Whether the instance is reachable
     * @return The updated instance
     */
    suspend fun updateInstanceHealth(
        domain: String,
        isOnline: Boolean,
    ): Either<StorageException, FederatedInstance> {
        return federationRepository.findInstanceByDomain(domain).flatMap { instance ->
            when {
                instance == null -> ItemNotFoundException(
                    message = "Instance not found: $domain",
                ).left()
                instance.status == InstanceStatus.BLOCKED -> instance.right()
                else -> federationRepository.updateInstanceStatus(
                    instance.id,
                    if (isOnline) InstanceStatus.ONLINE else InstanceStatus.OFFLINE,
                    if (isOnline) Clock.System.now() else null,
                )
            }
        }
    }

    // ========================================================================
    // Federated Sharing
    // ========================================================================

    /**
     * Create a federated share.
     *
     * @param input Share input
     * @param userId User creating the share
     * @return The created share
     */
    suspend fun createShare(
        input: CreateFederatedShareInput,
        userId: String,
    ): Either<StorageException, FederatedShare> {
        // Verify target instance is federated
        return federationRepository.findInstanceByDomain(input.targetInstance).flatMap { instance ->
            when {
                instance == null -> ItemNotFoundException(
                    message = "Not federated with ${input.targetInstance}",
                ).left()
                instance.status != InstanceStatus.ONLINE -> InvalidOperationException(
                    operation = "createShare",
                    message = "Instance ${input.targetInstance} is not online",
                ).left()
                !instance.capabilities.contains(FederationCapability.RECEIVE_SHARES) ->
                    InvalidOperationException(
                        operation = "createShare",
                        message = "Instance does not support receiving shares",
                    ).left()
                else -> {
                    val expiresAt = input.expiresInDays?.let { days ->
                        Clock.System.now().plus(days.days)
                    }

                    val share = FederatedShare(
                        id = UUID.randomUUID().toString(),
                        itemId = input.itemId,
                        sourceInstance = instanceConfig.domain,
                        targetInstance = input.targetInstance,
                        targetUserId = input.targetUserId,
                        permissions = input.permissions,
                        expiresAt = expiresAt,
                        createdBy = userId,
                        createdAt = Clock.System.now(),
                    )

                    federationRepository.createShare(share).flatMap { createdShare ->
                        // Record activity
                        recordActivity(
                            FederatedActivityType.SHARE_CREATED,
                            userId,
                            createdShare.id,
                            "share",
                            "Shared item with ${input.targetInstance}",
                        ).map { createdShare }
                    }
                }
            }
        }
    }

    /**
     * Accept an incoming share.
     *
     * @param shareId Share ID
     * @return The accepted share
     */
    suspend fun acceptShare(shareId: String): Either<StorageException, FederatedShare> {
        return federationRepository.findShare(shareId).flatMap { share ->
            when {
                share == null -> ItemNotFoundException(
                    itemId = shareId,
                    message = "Share not found: $shareId",
                ).left()
                share.status != FederatedShareStatus.PENDING -> InvalidOperationException(
                    operation = "acceptShare",
                    message = "Share is not pending",
                ).left()
                else -> federationRepository.updateShareStatus(
                    shareId,
                    FederatedShareStatus.ACCEPTED,
                    Clock.System.now(),
                )
            }
        }
    }

    /**
     * Decline an incoming share.
     *
     * @param shareId Share ID
     * @return The declined share
     */
    suspend fun declineShare(shareId: String): Either<StorageException, FederatedShare> {
        return federationRepository.findShare(shareId).flatMap { share ->
            when {
                share == null -> ItemNotFoundException(
                    itemId = shareId,
                    message = "Share not found: $shareId",
                ).left()
                share.status != FederatedShareStatus.PENDING -> InvalidOperationException(
                    operation = "acceptShare",
                    message = "Share is not pending",
                ).left()
                else -> federationRepository.updateShareStatus(
                    shareId,
                    FederatedShareStatus.DECLINED,
                )
            }
        }
    }

    /**
     * Revoke an outgoing share.
     *
     * @param shareId Share ID
     * @param userId User revoking (must be creator)
     * @return Unit on success
     */
    suspend fun revokeShare(
        shareId: String,
        userId: String,
    ): Either<StorageException, Unit> {
        return federationRepository.findShare(shareId).flatMap { share ->
            when {
                share == null -> ItemNotFoundException(
                    itemId = shareId,
                    message = "Share not found: $shareId",
                ).left()
                share.createdBy != userId -> AuthorizationException(
                    message = "Not authorized to revoke this share",
                ).left()
                else -> federationRepository.updateShareStatus(
                    shareId,
                    FederatedShareStatus.REVOKED,
                ).map { }
            }
        }
    }

    /**
     * Get outgoing shares for a user.
     *
     * @param userId User ID
     * @return List of outgoing shares
     */
    suspend fun getOutgoingShares(userId: String): Either<StorageException, List<FederatedShare>> {
        return federationRepository.getOutgoingShares(userId)
    }

    /**
     * Get incoming shares for an instance.
     *
     * @param status Filter by status
     * @return List of incoming shares
     */
    suspend fun getIncomingShares(
        status: FederatedShareStatus? = null,
    ): Either<StorageException, List<FederatedShare>> {
        return federationRepository.getIncomingShares(instanceConfig.domain, status)
    }

    // ========================================================================
    // Federated Identity
    // ========================================================================

    /**
     * Link a federated identity to a local user.
     *
     * @param localUserId Local user ID
     * @param remoteUserId Remote user ID
     * @param remoteInstance Remote instance domain
     * @param displayName Display name
     * @return The linked identity
     */
    suspend fun linkIdentity(
        localUserId: String,
        remoteUserId: String,
        remoteInstance: String,
        displayName: String,
    ): Either<StorageException, FederatedIdentity> {
        // Verify instance is federated
        return federationRepository.findInstanceByDomain(remoteInstance).flatMap { instance ->
            when {
                instance == null -> ItemNotFoundException(
                    message = "Not federated with $remoteInstance",
                ).left()
                !instance.capabilities.contains(FederationCapability.FEDERATED_IDENTITY) ->
                    InvalidOperationException(
                        operation = "linkIdentity",
                        message = "Instance does not support federated identity",
                    ).left()
                else -> {
                    val identity = FederatedIdentity(
                        id = UUID.randomUUID().toString(),
                        localUserId = localUserId,
                        remoteUserId = remoteUserId,
                        remoteInstance = remoteInstance,
                        displayName = displayName,
                        linkedAt = Clock.System.now(),
                    )
                    federationRepository.linkIdentity(identity)
                }
            }
        }
    }

    /**
     * Get linked identities for a local user.
     *
     * @param localUserId Local user ID
     * @return List of linked identities
     */
    suspend fun getLinkedIdentities(localUserId: String): Either<StorageException, List<FederatedIdentity>> {
        return federationRepository.getIdentitiesForUser(localUserId)
    }

    /**
     * Unlink a federated identity.
     *
     * @param identityId Identity ID
     * @return Unit on success
     */
    suspend fun unlinkIdentity(identityId: String): Either<StorageException, Unit> {
        return federationRepository.unlinkIdentity(identityId)
    }

    // ========================================================================
    // Activity Stream
    // ========================================================================

    /**
     * Record a federated activity.
     */
    private suspend fun recordActivity(
        activityType: FederatedActivityType,
        actorId: String,
        objectId: String,
        objectType: String,
        summary: String,
    ): Either<StorageException, FederatedActivity> {
        val activity = FederatedActivity(
            id = UUID.randomUUID().toString(),
            instanceDomain = instanceConfig.domain,
            activityType = activityType,
            actorId = actorId,
            objectId = objectId,
            objectType = objectType,
            summary = summary,
            timestamp = Clock.System.now(),
        )
        return federationRepository.recordActivity(activity)
    }

    /**
     * Get activities from federated instances.
     *
     * @param instanceDomain Filter by instance (null for all)
     * @param since Activities since this timestamp
     * @param limit Maximum number of activities
     * @return List of activities
     */
    suspend fun getActivities(
        instanceDomain: String? = null,
        since: Instant? = null,
        limit: Int = 100,
    ): Either<StorageException, List<FederatedActivity>> {
        return if (instanceDomain != null) {
            federationRepository.getActivitiesFromInstance(instanceDomain, since, limit)
        } else {
            federationRepository.listInstances().flatMap { instances ->
                val activities = mutableListOf<FederatedActivity>()
                for (instance in instances) {
                    federationRepository.getActivitiesFromInstance(
                        instance.domain,
                        since,
                        limit / instances.size,
                    ).fold(
                        { return it.left() },
                        { activities.addAll(it) },
                    )
                }
                activities.sortedByDescending { it.timestamp }.right()
            }
        }
    }

    /**
     * Stream federated activities.
     *
     * @param instanceDomains Instances to watch (empty for all)
     * @return Flow of activities
     */
    fun streamActivities(instanceDomains: List<String> = emptyList()): Flow<FederatedActivity> {
        return federationRepository.streamActivities(instanceDomains)
    }

    // ========================================================================
    // Message Signing
    // ========================================================================

    private val cryptoService = FederationCryptoService(
        privateKeyBase64 = instanceConfig.privateKey.takeIf { it.length > 50 },
        publicKeyBase64 = instanceConfig.publicKey.takeIf { it.length > 50 },
    )

    /**
     * Get the public key for this instance.
     */
    fun getPublicKey(): String? = getFederationPublicKey(cryptoService)

    /**
     * Sign a message for federation communication.
     */
    fun signMessage(payload: String): SignedFederationMessage =
        signFederationMessage(cryptoService, instanceConfig, payload)

    /**
     * Verify a signed message from another instance.
     */
    suspend fun verifyMessage(message: SignedFederationMessage): Either<StorageException, Boolean> =
        verifyFederationMessage(federationRepository, cryptoService, message)

    // ========================================================================
    // Maintenance
    // ========================================================================

    /**
     * Run health checks on all federated instances.
     */
    suspend fun runHealthChecks(): Either<StorageException, Map<String, Boolean>> =
        runFederationHealthChecks(federationRepository) { domain, isOnline ->
            updateInstanceHealth(domain, isOnline)
        }

    /**
     * Clean up expired shares and old activities.
     */
    suspend fun cleanup(olderThanDays: Int = 30): Either<StorageException, Int> =
        cleanupFederation(federationRepository, olderThanDays)
}
