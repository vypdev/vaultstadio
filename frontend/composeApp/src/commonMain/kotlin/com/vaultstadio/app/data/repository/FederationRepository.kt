/**
 * Federation Repository
 */

package com.vaultstadio.app.data.repository

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.service.FederationService
import com.vaultstadio.app.domain.model.FederatedActivity
import com.vaultstadio.app.domain.model.FederatedIdentity
import com.vaultstadio.app.domain.model.FederatedInstance
import com.vaultstadio.app.domain.model.FederatedShare
import com.vaultstadio.app.domain.model.FederatedShareStatus
import com.vaultstadio.app.domain.model.InstanceStatus
import com.vaultstadio.app.domain.model.SharePermission
import kotlinx.datetime.Instant
import org.koin.core.annotation.Single

/**
 * Repository interface for federation operations.
 */
interface FederationRepository {
    suspend fun requestFederation(targetDomain: String, message: String? = null): ApiResult<FederatedInstance>
    suspend fun getInstances(status: InstanceStatus? = null): ApiResult<List<FederatedInstance>>
    suspend fun getInstance(domain: String): ApiResult<FederatedInstance>
    suspend fun blockInstance(instanceId: String): ApiResult<Unit>
    suspend fun removeInstance(instanceId: String): ApiResult<Unit>
    suspend fun createShare(
        itemId: String,
        targetInstance: String,
        targetUserId: String?,
        permissions: List<SharePermission>,
        expiresInDays: Int?,
    ): ApiResult<FederatedShare>
    suspend fun getOutgoingShares(): ApiResult<List<FederatedShare>>
    suspend fun getIncomingShares(status: FederatedShareStatus? = null): ApiResult<List<FederatedShare>>
    suspend fun acceptShare(shareId: String): ApiResult<Unit>
    suspend fun declineShare(shareId: String): ApiResult<Unit>
    suspend fun revokeShare(shareId: String): ApiResult<Unit>
    suspend fun linkIdentity(
        remoteUserId: String,
        remoteInstance: String,
        displayName: String,
    ): ApiResult<FederatedIdentity>
    suspend fun getIdentities(): ApiResult<List<FederatedIdentity>>
    suspend fun unlinkIdentity(identityId: String): ApiResult<Unit>
    suspend fun getActivities(
        instance: String? = null,
        since: Instant? = null,
        limit: Int = 100,
    ): ApiResult<List<FederatedActivity>>
}

@Single(binds = [FederationRepository::class])
class FederationRepositoryImpl(
    private val service: FederationService,
) : FederationRepository {

    override suspend fun requestFederation(
        targetDomain: String,
        message: String?,
    ) = service.requestFederation(targetDomain, message)

    override suspend fun getInstances(status: InstanceStatus?) = service.getInstances(status)
    override suspend fun getInstance(domain: String) = service.getInstance(domain)
    override suspend fun blockInstance(instanceId: String) = service.blockInstance(instanceId)
    override suspend fun removeInstance(instanceId: String) = service.removeInstance(instanceId)

    override suspend fun createShare(
        itemId: String,
        targetInstance: String,
        targetUserId: String?,
        permissions: List<SharePermission>,
        expiresInDays: Int?,
    ) = service.createShare(itemId, targetInstance, targetUserId, permissions, expiresInDays)

    override suspend fun getOutgoingShares() = service.getOutgoingShares()
    override suspend fun getIncomingShares(status: FederatedShareStatus?) = service.getIncomingShares(status)
    override suspend fun acceptShare(shareId: String) = service.acceptShare(shareId)
    override suspend fun declineShare(shareId: String) = service.declineShare(shareId)
    override suspend fun revokeShare(shareId: String) = service.revokeShare(shareId)

    override suspend fun linkIdentity(
        remoteUserId: String,
        remoteInstance: String,
        displayName: String,
    ) = service.linkIdentity(remoteUserId, remoteInstance, displayName)

    override suspend fun getIdentities() = service.getIdentities()
    override suspend fun unlinkIdentity(identityId: String) = service.unlinkIdentity(identityId)

    override suspend fun getActivities(
        instance: String?,
        since: Instant?,
        limit: Int,
    ) = service.getActivities(instance, since, limit)
}
