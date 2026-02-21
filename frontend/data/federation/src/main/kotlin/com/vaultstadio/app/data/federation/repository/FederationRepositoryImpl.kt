/**
 * Federation Repository implementation
 */

package com.vaultstadio.app.data.federation.repository

import com.vaultstadio.app.data.federation.service.FederationService
import com.vaultstadio.app.data.network.mapper.toResult
import com.vaultstadio.app.domain.federation.FederationRepository
import com.vaultstadio.app.domain.federation.model.FederatedActivity
import com.vaultstadio.app.domain.federation.model.FederatedIdentity
import com.vaultstadio.app.domain.federation.model.FederatedInstance
import com.vaultstadio.app.domain.federation.model.FederatedShare
import com.vaultstadio.app.domain.federation.model.FederatedShareStatus
import com.vaultstadio.app.domain.federation.model.InstanceStatus
import com.vaultstadio.app.domain.federation.model.SharePermission
import com.vaultstadio.app.domain.result.Result
import kotlinx.datetime.Instant

class FederationRepositoryImpl(
    private val service: FederationService,
) : FederationRepository {

    override suspend fun requestFederation(targetDomain: String, message: String?) =
        service.requestFederation(targetDomain, message).toResult()

    override suspend fun getInstances(status: InstanceStatus?) =
        service.getInstances(status).toResult()

    override suspend fun getInstance(domain: String) =
        service.getInstance(domain).toResult()

    override suspend fun blockInstance(instanceId: String) =
        service.blockInstance(instanceId).toResult()

    override suspend fun removeInstance(instanceId: String) =
        service.removeInstance(instanceId).toResult()

    override suspend fun createShare(
        itemId: String,
        targetInstance: String,
        targetUserId: String?,
        permissions: List<SharePermission>,
        expiresInDays: Int?,
    ) = service.createShare(itemId, targetInstance, targetUserId, permissions, expiresInDays).toResult()

    override suspend fun getOutgoingShares() =
        service.getOutgoingShares().toResult()

    override suspend fun getIncomingShares(status: FederatedShareStatus?) =
        service.getIncomingShares(status).toResult()

    override suspend fun acceptShare(shareId: String) =
        service.acceptShare(shareId).toResult()

    override suspend fun declineShare(shareId: String) =
        service.declineShare(shareId).toResult()

    override suspend fun revokeShare(shareId: String) =
        service.revokeShare(shareId).toResult()

    override suspend fun linkIdentity(
        remoteUserId: String,
        remoteInstance: String,
        displayName: String,
    ) = service.linkIdentity(remoteUserId, remoteInstance, displayName).toResult()

    override suspend fun getIdentities() =
        service.getIdentities().toResult()

    override suspend fun unlinkIdentity(identityId: String) =
        service.unlinkIdentity(identityId).toResult()

    override suspend fun getActivities(
        instance: String?,
        since: Instant?,
        limit: Int,
    ): Result<List<FederatedActivity>> =
        service.getActivities(instance, since, limit).toResult()
}
