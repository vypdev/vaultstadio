/**
 * VaultStadio Exposed Federation Repository Implementation
 */

package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.FederatedActivity
import com.vaultstadio.core.domain.model.FederatedIdentity
import com.vaultstadio.core.domain.model.FederatedInstance
import com.vaultstadio.core.domain.model.FederatedShare
import com.vaultstadio.core.domain.model.FederatedShareStatus
import com.vaultstadio.core.domain.model.FederationCapability
import com.vaultstadio.core.domain.model.InstanceStatus
import com.vaultstadio.core.domain.repository.FederationRepository
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.infrastructure.persistence.entities.FederatedActivitiesTable
import com.vaultstadio.infrastructure.persistence.entities.FederatedIdentitiesTable
import com.vaultstadio.infrastructure.persistence.entities.FederatedInstancesTable
import com.vaultstadio.infrastructure.persistence.entities.FederatedSharesTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

/**
 * Exposed implementation of FederationRepository.
 */
class ExposedFederationRepository : FederationRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    // ========================================================================
    // Instance Management
    // ========================================================================

    override suspend fun registerInstance(instance: FederatedInstance): Either<StorageException, FederatedInstance> {
        return try {
            dbQuery {
                FederatedInstancesTable.insert {
                    it[id] = instance.id
                    it[domain] = instance.domain
                    it[name] = instance.name
                    it[description] = instance.description
                    it[version] = instance.version
                    it[publicKey] = instance.publicKey
                    it[capabilities] = json.encodeToString(instance.capabilities.map { c -> c.name })
                    it[status] = instance.status.name
                    it[lastSeenAt] = instance.lastSeenAt
                    it[registeredAt] = instance.registeredAt
                    it[metadata] = if (instance.metadata.isNotEmpty()) {
                        json.encodeToString(instance.metadata)
                    } else {
                        null
                    }
                }
            }
            instance.right()
        } catch (e: Exception) {
            DatabaseException("Failed to register instance: ${e.message}", e).left()
        }
    }

    override suspend fun findInstance(instanceId: String): Either<StorageException, FederatedInstance?> {
        return try {
            dbQuery {
                FederatedInstancesTable.selectAll()
                    .where { FederatedInstancesTable.id eq instanceId }
                    .map { it.toFederatedInstance() }
                    .singleOrNull()
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to find instance: ${e.message}", e).left()
        }
    }

    override suspend fun findInstanceByDomain(domain: String): Either<StorageException, FederatedInstance?> {
        return try {
            dbQuery {
                FederatedInstancesTable.selectAll()
                    .where { FederatedInstancesTable.domain eq domain }
                    .map { it.toFederatedInstance() }
                    .singleOrNull()
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to find instance by domain: ${e.message}", e).left()
        }
    }

    override suspend fun listInstances(
        status: InstanceStatus?,
        capability: FederationCapability?,
    ): Either<StorageException, List<FederatedInstance>> {
        return try {
            dbQuery {
                var query = FederatedInstancesTable.selectAll()

                if (status != null) {
                    query = query.andWhere { FederatedInstancesTable.status eq status.name }
                }

                val instances = query.map { it.toFederatedInstance() }

                if (capability != null) {
                    instances.filter { it.capabilities.contains(capability) }
                } else {
                    instances
                }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to list instances: ${e.message}", e).left()
        }
    }

    override suspend fun updateInstance(instance: FederatedInstance): Either<StorageException, FederatedInstance> {
        return try {
            dbQuery {
                FederatedInstancesTable.update({ FederatedInstancesTable.id eq instance.id }) {
                    it[name] = instance.name
                    it[description] = instance.description
                    it[version] = instance.version
                    it[capabilities] = json.encodeToString(instance.capabilities.map { c -> c.name })
                    it[status] = instance.status.name
                    it[lastSeenAt] = instance.lastSeenAt
                }
            }
            instance.right()
        } catch (e: Exception) {
            DatabaseException("Failed to update instance: ${e.message}", e).left()
        }
    }

    override suspend fun updateInstanceStatus(
        instanceId: String,
        status: InstanceStatus,
        lastSeenAt: Instant?,
    ): Either<StorageException, FederatedInstance> {
        return try {
            dbQuery {
                FederatedInstancesTable.update({ FederatedInstancesTable.id eq instanceId }) {
                    it[FederatedInstancesTable.status] = status.name
                    if (lastSeenAt != null) {
                        it[FederatedInstancesTable.lastSeenAt] = lastSeenAt
                    }
                }
            }
            findInstance(instanceId).fold(
                { it.left() },
                { instance -> instance?.right() ?: DatabaseException("Instance not found").left() },
            )
        } catch (e: Exception) {
            DatabaseException("Failed to update instance status: ${e.message}", e).left()
        }
    }

    override suspend fun removeInstance(instanceId: String): Either<StorageException, Unit> {
        return try {
            dbQuery {
                FederatedInstancesTable.deleteWhere { FederatedInstancesTable.id eq instanceId }
            }
            Unit.right()
        } catch (e: Exception) {
            DatabaseException("Failed to remove instance: ${e.message}", e).left()
        }
    }

    override suspend fun getStaleInstances(notSeenSince: Instant): Either<StorageException, List<FederatedInstance>> {
        return try {
            dbQuery {
                FederatedInstancesTable.selectAll()
                    .where {
                        (FederatedInstancesTable.lastSeenAt less notSeenSince) or
                            (FederatedInstancesTable.lastSeenAt.isNull())
                    }
                    .map { it.toFederatedInstance() }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to get stale instances: ${e.message}", e).left()
        }
    }

    // ========================================================================
    // Federated Shares
    // ========================================================================

    override suspend fun createShare(share: FederatedShare): Either<StorageException, FederatedShare> {
        return try {
            dbQuery {
                FederatedSharesTable.insert {
                    it[id] = share.id
                    it[itemId] = share.itemId
                    it[sourceInstance] = share.sourceInstance
                    it[targetInstance] = share.targetInstance
                    it[targetUserId] = share.targetUserId
                    it[permissions] = json.encodeToString(share.permissions.map { p -> p.name })
                    it[expiresAt] = share.expiresAt
                    it[createdBy] = share.createdBy
                    it[createdAt] = share.createdAt
                    it[acceptedAt] = share.acceptedAt
                    it[status] = share.status.name
                }
            }
            share.right()
        } catch (e: Exception) {
            DatabaseException("Failed to create share: ${e.message}", e).left()
        }
    }

    override suspend fun findShare(shareId: String): Either<StorageException, FederatedShare?> {
        return try {
            dbQuery {
                FederatedSharesTable.selectAll()
                    .where { FederatedSharesTable.id eq shareId }
                    .map { it.toFederatedShare() }
                    .singleOrNull()
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to find share: ${e.message}", e).left()
        }
    }

    override suspend fun getOutgoingShares(
        userId: String,
        status: FederatedShareStatus?,
    ): Either<StorageException, List<FederatedShare>> {
        return try {
            dbQuery {
                var query = FederatedSharesTable.selectAll()
                    .where { FederatedSharesTable.createdBy eq userId }

                if (status != null) {
                    query = query.andWhere { FederatedSharesTable.status eq status.name }
                }

                query.map { it.toFederatedShare() }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to get outgoing shares: ${e.message}", e).left()
        }
    }

    override suspend fun getIncomingShares(
        instanceDomain: String,
        status: FederatedShareStatus?,
    ): Either<StorageException, List<FederatedShare>> {
        return try {
            dbQuery {
                var query = FederatedSharesTable.selectAll()
                    .where { FederatedSharesTable.targetInstance eq instanceDomain }

                if (status != null) {
                    query = query.andWhere { FederatedSharesTable.status eq status.name }
                }

                query.map { it.toFederatedShare() }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to get incoming shares: ${e.message}", e).left()
        }
    }

    override suspend fun getSharesForItem(itemId: String): Either<StorageException, List<FederatedShare>> {
        return try {
            dbQuery {
                FederatedSharesTable.selectAll()
                    .where { FederatedSharesTable.itemId eq itemId }
                    .map { it.toFederatedShare() }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to get shares for item: ${e.message}", e).left()
        }
    }

    override suspend fun updateShare(share: FederatedShare): Either<StorageException, FederatedShare> {
        return try {
            dbQuery {
                FederatedSharesTable.update({ FederatedSharesTable.id eq share.id }) {
                    it[permissions] = json.encodeToString(share.permissions.map { p -> p.name })
                    it[expiresAt] = share.expiresAt
                    it[acceptedAt] = share.acceptedAt
                    it[status] = share.status.name
                }
            }
            share.right()
        } catch (e: Exception) {
            DatabaseException("Failed to update share: ${e.message}", e).left()
        }
    }

    override suspend fun updateShareStatus(
        shareId: String,
        status: FederatedShareStatus,
        acceptedAt: Instant?,
    ): Either<StorageException, FederatedShare> {
        return try {
            dbQuery {
                FederatedSharesTable.update({ FederatedSharesTable.id eq shareId }) {
                    it[FederatedSharesTable.status] = status.name
                    if (acceptedAt != null) {
                        it[FederatedSharesTable.acceptedAt] = acceptedAt
                    }
                }
            }
            findShare(shareId).fold(
                { it.left() },
                { share -> share?.right() ?: DatabaseException("Share not found").left() },
            )
        } catch (e: Exception) {
            DatabaseException("Failed to update share status: ${e.message}", e).left()
        }
    }

    override suspend fun deleteShare(shareId: String): Either<StorageException, Unit> {
        return try {
            dbQuery {
                FederatedSharesTable.deleteWhere { FederatedSharesTable.id eq shareId }
            }
            Unit.right()
        } catch (e: Exception) {
            DatabaseException("Failed to delete share: ${e.message}", e).left()
        }
    }

    override suspend fun getExpiredShares(expiredBefore: Instant): Either<StorageException, List<FederatedShare>> {
        return try {
            dbQuery {
                FederatedSharesTable.selectAll()
                    .where {
                        (FederatedSharesTable.expiresAt less expiredBefore) and
                            (FederatedSharesTable.status neq FederatedShareStatus.EXPIRED.name)
                    }
                    .map { it.toFederatedShare() }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to get expired shares: ${e.message}", e).left()
        }
    }

    // ========================================================================
    // Federated Identities
    // ========================================================================

    override suspend fun linkIdentity(identity: FederatedIdentity): Either<StorageException, FederatedIdentity> {
        return try {
            dbQuery {
                FederatedIdentitiesTable.insert {
                    it[id] = identity.id
                    it[localUserId] = identity.localUserId
                    it[remoteUserId] = identity.remoteUserId
                    it[remoteInstance] = identity.remoteInstance
                    it[displayName] = identity.displayName
                    it[email] = identity.email
                    it[avatarUrl] = identity.avatarUrl
                    it[verified] = identity.verified
                    it[linkedAt] = identity.linkedAt
                }
            }
            identity.right()
        } catch (e: Exception) {
            DatabaseException("Failed to link identity: ${e.message}", e).left()
        }
    }

    override suspend fun findIdentity(identityId: String): Either<StorageException, FederatedIdentity?> {
        return try {
            dbQuery {
                FederatedIdentitiesTable.selectAll()
                    .where { FederatedIdentitiesTable.id eq identityId }
                    .map { it.toFederatedIdentity() }
                    .singleOrNull()
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to find identity: ${e.message}", e).left()
        }
    }

    override suspend fun findIdentityByFederatedId(federatedId: String): Either<StorageException, FederatedIdentity?> {
        return try {
            val parts = federatedId.split("@", limit = 2)
            if (parts.size != 2) {
                return null.right()
            }
            val (remoteUserId, remoteInstance) = parts

            dbQuery {
                FederatedIdentitiesTable.selectAll()
                    .where {
                        (FederatedIdentitiesTable.remoteUserId eq remoteUserId) and
                            (FederatedIdentitiesTable.remoteInstance eq remoteInstance)
                    }
                    .map { it.toFederatedIdentity() }
                    .singleOrNull()
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to find identity: ${e.message}", e).left()
        }
    }

    override suspend fun getIdentitiesForUser(localUserId: String): Either<StorageException, List<FederatedIdentity>> {
        return try {
            dbQuery {
                FederatedIdentitiesTable.selectAll()
                    .where { FederatedIdentitiesTable.localUserId eq localUserId }
                    .map { it.toFederatedIdentity() }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to get identities: ${e.message}", e).left()
        }
    }

    override suspend fun getIdentitiesFromInstance(
        instanceDomain: String,
    ): Either<StorageException, List<FederatedIdentity>> {
        return try {
            dbQuery {
                FederatedIdentitiesTable.selectAll()
                    .where { FederatedIdentitiesTable.remoteInstance eq instanceDomain }
                    .map { it.toFederatedIdentity() }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to get identities from instance: ${e.message}", e).left()
        }
    }

    override suspend fun updateIdentity(identity: FederatedIdentity): Either<StorageException, FederatedIdentity> {
        return try {
            dbQuery {
                FederatedIdentitiesTable.update({ FederatedIdentitiesTable.id eq identity.id }) {
                    it[displayName] = identity.displayName
                    it[email] = identity.email
                    it[avatarUrl] = identity.avatarUrl
                    it[verified] = identity.verified
                }
            }
            identity.right()
        } catch (e: Exception) {
            DatabaseException("Failed to update identity: ${e.message}", e).left()
        }
    }

    override suspend fun unlinkIdentity(identityId: String): Either<StorageException, Unit> {
        return try {
            dbQuery {
                FederatedIdentitiesTable.deleteWhere { FederatedIdentitiesTable.id eq identityId }
            }
            Unit.right()
        } catch (e: Exception) {
            DatabaseException("Failed to unlink identity: ${e.message}", e).left()
        }
    }

    // ========================================================================
    // Federated Activity
    // ========================================================================

    override suspend fun recordActivity(activity: FederatedActivity): Either<StorageException, FederatedActivity> {
        return try {
            dbQuery {
                FederatedActivitiesTable.insert {
                    it[id] = activity.id
                    it[instanceDomain] = activity.instanceDomain
                    it[activityType] = activity.activityType.name
                    it[actorId] = activity.actorId
                    it[objectId] = activity.objectId
                    it[objectType] = activity.objectType
                    it[summary] = activity.summary
                    it[timestamp] = activity.timestamp
                    it[metadata] = if (activity.metadata.isNotEmpty()) {
                        json.encodeToString(activity.metadata)
                    } else {
                        null
                    }
                }
            }
            activity.right()
        } catch (e: Exception) {
            DatabaseException("Failed to record activity: ${e.message}", e).left()
        }
    }

    override suspend fun getActivitiesFromInstance(
        instanceDomain: String,
        since: Instant?,
        limit: Int,
    ): Either<StorageException, List<FederatedActivity>> {
        return try {
            dbQuery {
                var query = FederatedActivitiesTable.selectAll()
                    .where { FederatedActivitiesTable.instanceDomain eq instanceDomain }

                if (since != null) {
                    query = query.andWhere { FederatedActivitiesTable.timestamp greater since }
                }

                query.orderBy(FederatedActivitiesTable.timestamp, SortOrder.DESC)
                    .limit(limit)
                    .map { it.toFederatedActivity() }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to get activities: ${e.message}", e).left()
        }
    }

    override suspend fun getActivitiesForActor(
        actorId: String,
        limit: Int,
    ): Either<StorageException, List<FederatedActivity>> {
        return try {
            dbQuery {
                FederatedActivitiesTable.selectAll()
                    .where { FederatedActivitiesTable.actorId eq actorId }
                    .orderBy(FederatedActivitiesTable.timestamp, SortOrder.DESC)
                    .limit(limit)
                    .map { it.toFederatedActivity() }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to get activities for actor: ${e.message}", e).left()
        }
    }

    override fun streamActivities(instanceDomains: List<String>): Flow<FederatedActivity> = flow {
        // Real-time streaming would use a different mechanism
    }

    override suspend fun pruneActivities(before: Instant): Either<StorageException, Int> {
        return try {
            dbQuery {
                FederatedActivitiesTable.deleteWhere { timestamp less before }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to prune activities: ${e.message}", e).left()
        }
    }
}
