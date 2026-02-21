/**
 * VaultStadio Federation Repository
 *
 * Interface for federation persistence operations.
 */

package com.vaultstadio.core.domain.repository

import arrow.core.Either
import com.vaultstadio.core.domain.model.FederatedActivity
import com.vaultstadio.core.domain.model.FederatedIdentity
import com.vaultstadio.core.domain.model.FederatedInstance
import com.vaultstadio.core.domain.model.FederatedShare
import com.vaultstadio.core.domain.model.FederatedShareStatus
import com.vaultstadio.core.domain.model.FederationCapability
import com.vaultstadio.core.domain.model.InstanceStatus
import com.vaultstadio.domain.common.exception.StorageException
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Repository interface for federation features.
 */
interface FederationRepository {

    // ========================================================================
    // Instance Management
    // ========================================================================

    /**
     * Register a new federated instance.
     *
     * @param instance The instance to register
     * @return The registered instance
     */
    suspend fun registerInstance(instance: FederatedInstance): Either<StorageException, FederatedInstance>

    /**
     * Find an instance by ID.
     *
     * @param instanceId Instance ID
     * @return The instance or null
     */
    suspend fun findInstance(instanceId: String): Either<StorageException, FederatedInstance?>

    /**
     * Find an instance by domain.
     *
     * @param domain Instance domain
     * @return The instance or null
     */
    suspend fun findInstanceByDomain(domain: String): Either<StorageException, FederatedInstance?>

    /**
     * List all registered instances.
     *
     * @param status Filter by status (null for all)
     * @param capability Filter by capability (null for all)
     * @return List of instances
     */
    suspend fun listInstances(
        status: InstanceStatus? = null,
        capability: FederationCapability? = null,
    ): Either<StorageException, List<FederatedInstance>>

    /**
     * Update an instance.
     *
     * @param instance Updated instance
     * @return The updated instance
     */
    suspend fun updateInstance(instance: FederatedInstance): Either<StorageException, FederatedInstance>

    /**
     * Update instance status.
     *
     * @param instanceId Instance ID
     * @param status New status
     * @param lastSeenAt Last seen timestamp
     * @return The updated instance
     */
    suspend fun updateInstanceStatus(
        instanceId: String,
        status: InstanceStatus,
        lastSeenAt: Instant? = null,
    ): Either<StorageException, FederatedInstance>

    /**
     * Remove an instance.
     *
     * @param instanceId Instance ID
     * @return Unit on success
     */
    suspend fun removeInstance(instanceId: String): Either<StorageException, Unit>

    /**
     * Get instances that haven't been seen recently.
     *
     * @param notSeenSince Threshold timestamp
     * @return List of stale instances
     */
    suspend fun getStaleInstances(notSeenSince: Instant): Either<StorageException, List<FederatedInstance>>

    // ========================================================================
    // Federated Shares
    // ========================================================================

    /**
     * Create a federated share.
     *
     * @param share The share to create
     * @return The created share
     */
    suspend fun createShare(share: FederatedShare): Either<StorageException, FederatedShare>

    /**
     * Find a share by ID.
     *
     * @param shareId Share ID
     * @return The share or null
     */
    suspend fun findShare(shareId: String): Either<StorageException, FederatedShare?>

    /**
     * Get outgoing shares (shares created by local users).
     *
     * @param userId User ID
     * @param status Filter by status (null for all)
     * @return List of outgoing shares
     */
    suspend fun getOutgoingShares(
        userId: String,
        status: FederatedShareStatus? = null,
    ): Either<StorageException, List<FederatedShare>>

    /**
     * Get incoming shares (shares received from other instances).
     *
     * @param instanceDomain Source instance domain
     * @param status Filter by status (null for all)
     * @return List of incoming shares
     */
    suspend fun getIncomingShares(
        instanceDomain: String,
        status: FederatedShareStatus? = null,
    ): Either<StorageException, List<FederatedShare>>

    /**
     * Get shares for an item.
     *
     * @param itemId Storage item ID
     * @return List of shares for this item
     */
    suspend fun getSharesForItem(itemId: String): Either<StorageException, List<FederatedShare>>

    /**
     * Update a share.
     *
     * @param share Updated share
     * @return The updated share
     */
    suspend fun updateShare(share: FederatedShare): Either<StorageException, FederatedShare>

    /**
     * Update share status.
     *
     * @param shareId Share ID
     * @param status New status
     * @param acceptedAt When it was accepted (if applicable)
     * @return The updated share
     */
    suspend fun updateShareStatus(
        shareId: String,
        status: FederatedShareStatus,
        acceptedAt: Instant? = null,
    ): Either<StorageException, FederatedShare>

    /**
     * Delete a share.
     *
     * @param shareId Share ID
     * @return Unit on success
     */
    suspend fun deleteShare(shareId: String): Either<StorageException, Unit>

    /**
     * Get expired shares.
     *
     * @param expiredBefore Shares expired before this timestamp
     * @return List of expired shares
     */
    suspend fun getExpiredShares(expiredBefore: Instant): Either<StorageException, List<FederatedShare>>

    // ========================================================================
    // Federated Identities
    // ========================================================================

    /**
     * Link a federated identity.
     *
     * @param identity The identity to link
     * @return The linked identity
     */
    suspend fun linkIdentity(identity: FederatedIdentity): Either<StorageException, FederatedIdentity>

    /**
     * Find an identity by ID.
     *
     * @param identityId Identity ID
     * @return The identity or null
     */
    suspend fun findIdentity(identityId: String): Either<StorageException, FederatedIdentity?>

    /**
     * Find identity by federated ID (user@instance).
     *
     * @param federatedId Federated ID
     * @return The identity or null
     */
    suspend fun findIdentityByFederatedId(federatedId: String): Either<StorageException, FederatedIdentity?>

    /**
     * Get identities linked to a local user.
     *
     * @param localUserId Local user ID
     * @return List of linked identities
     */
    suspend fun getIdentitiesForUser(localUserId: String): Either<StorageException, List<FederatedIdentity>>

    /**
     * Get identities from a specific instance.
     *
     * @param instanceDomain Instance domain
     * @return List of identities from that instance
     */
    suspend fun getIdentitiesFromInstance(instanceDomain: String): Either<StorageException, List<FederatedIdentity>>

    /**
     * Update an identity.
     *
     * @param identity Updated identity
     * @return The updated identity
     */
    suspend fun updateIdentity(identity: FederatedIdentity): Either<StorageException, FederatedIdentity>

    /**
     * Unlink an identity.
     *
     * @param identityId Identity ID
     * @return Unit on success
     */
    suspend fun unlinkIdentity(identityId: String): Either<StorageException, Unit>

    // ========================================================================
    // Federated Activity
    // ========================================================================

    /**
     * Record a federated activity.
     *
     * @param activity The activity to record
     * @return The recorded activity
     */
    suspend fun recordActivity(activity: FederatedActivity): Either<StorageException, FederatedActivity>

    /**
     * Get activities from an instance.
     *
     * @param instanceDomain Instance domain
     * @param since Activities since this timestamp
     * @param limit Maximum number of activities
     * @return List of activities
     */
    suspend fun getActivitiesFromInstance(
        instanceDomain: String,
        since: Instant? = null,
        limit: Int = 100,
    ): Either<StorageException, List<FederatedActivity>>

    /**
     * Get activities for an actor.
     *
     * @param actorId Actor federated ID
     * @param limit Maximum number of activities
     * @return List of activities
     */
    suspend fun getActivitiesForActor(
        actorId: String,
        limit: Int = 100,
    ): Either<StorageException, List<FederatedActivity>>

    /**
     * Stream federated activities.
     *
     * @param instanceDomains Instance domains to watch (empty for all)
     * @return Flow of activities
     */
    fun streamActivities(instanceDomains: List<String> = emptyList()): Flow<FederatedActivity>

    /**
     * Prune old activities.
     *
     * @param before Delete activities before this timestamp
     * @return Number of activities deleted
     */
    suspend fun pruneActivities(before: Instant): Either<StorageException, Int>
}
