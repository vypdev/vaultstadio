/**
 * Federation Service
 */

package com.vaultstadio.app.data.federation.service

import com.vaultstadio.app.data.federation.api.FederationApi
import com.vaultstadio.app.data.federation.dto.CreateFederatedShareRequestDTO
import com.vaultstadio.app.data.federation.dto.LinkIdentityRequestDTO
import com.vaultstadio.app.data.federation.mapper.toDomain
import com.vaultstadio.app.data.federation.mapper.toFederatedActivityList
import com.vaultstadio.app.data.federation.mapper.toFederatedShareList
import com.vaultstadio.app.data.federation.mapper.toIdentityList
import com.vaultstadio.app.data.federation.mapper.toInstanceList
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.federation.model.FederatedActivity
import com.vaultstadio.app.domain.federation.model.FederatedIdentity
import com.vaultstadio.app.domain.federation.model.FederatedInstance
import com.vaultstadio.app.domain.federation.model.FederatedShare
import com.vaultstadio.app.domain.federation.model.FederatedShareStatus
import com.vaultstadio.app.domain.federation.model.InstanceStatus
import com.vaultstadio.app.domain.federation.model.SharePermission
import kotlinx.datetime.Instant

class FederationService(private val api: FederationApi) {

    suspend fun requestFederation(targetDomain: String, message: String? = null): ApiResult<FederatedInstance> =
        api.requestFederation(targetDomain, message).map { it.toDomain() }

    suspend fun getInstances(status: InstanceStatus? = null): ApiResult<List<FederatedInstance>> =
        api.getInstances(status?.name).map { it.toInstanceList() }

    suspend fun getInstance(domain: String): ApiResult<FederatedInstance> =
        api.getInstance(domain).map { it.toDomain() }

    suspend fun blockInstance(instanceId: String): ApiResult<Unit> =
        api.blockInstance(instanceId)

    suspend fun removeInstance(instanceId: String): ApiResult<Unit> =
        api.removeInstance(instanceId)

    suspend fun createShare(
        itemId: String,
        targetInstance: String,
        targetUserId: String?,
        permissions: List<SharePermission>,
        expiresInDays: Int?,
    ): ApiResult<FederatedShare> =
        api.createShare(
            CreateFederatedShareRequestDTO(
                itemId,
                targetInstance,
                targetUserId,
                permissions.map { it.name },
                expiresInDays,
            ),
        ).map { it.toDomain() }

    suspend fun getOutgoingShares(): ApiResult<List<FederatedShare>> =
        api.getOutgoingShares().map { it.toFederatedShareList() }

    suspend fun getIncomingShares(status: FederatedShareStatus? = null): ApiResult<List<FederatedShare>> =
        api.getIncomingShares(status?.name).map { it.toFederatedShareList() }

    suspend fun acceptShare(shareId: String): ApiResult<Unit> =
        api.acceptShare(shareId)

    suspend fun declineShare(shareId: String): ApiResult<Unit> =
        api.declineShare(shareId)

    suspend fun revokeShare(shareId: String): ApiResult<Unit> =
        api.revokeShare(shareId)

    suspend fun linkIdentity(
        remoteUserId: String,
        remoteInstance: String,
        displayName: String,
    ): ApiResult<FederatedIdentity> =
        api.linkIdentity(LinkIdentityRequestDTO(remoteUserId, remoteInstance, displayName)).map { it.toDomain() }

    suspend fun getIdentities(): ApiResult<List<FederatedIdentity>> =
        api.getIdentities().map { it.toIdentityList() }

    suspend fun unlinkIdentity(identityId: String): ApiResult<Unit> =
        api.unlinkIdentity(identityId)

    suspend fun getActivities(
        instance: String? = null,
        since: Instant? = null,
        limit: Int = 100,
    ): ApiResult<List<FederatedActivity>> =
        api.getActivities(instance, since, limit).map { it.toFederatedActivityList() }
}
