/**
 * Repository interface for federation operations.
 */

package com.vaultstadio.app.domain.federation

import com.vaultstadio.app.domain.federation.model.FederatedActivity
import com.vaultstadio.app.domain.federation.model.FederatedIdentity
import com.vaultstadio.app.domain.federation.model.FederatedInstance
import com.vaultstadio.app.domain.federation.model.FederatedShare
import com.vaultstadio.app.domain.federation.model.FederatedShareStatus
import com.vaultstadio.app.domain.federation.model.InstanceStatus
import com.vaultstadio.app.domain.federation.model.SharePermission
import com.vaultstadio.app.domain.result.Result
import kotlinx.datetime.Instant

interface FederationRepository {
    suspend fun requestFederation(targetDomain: String, message: String? = null): Result<FederatedInstance>
    suspend fun getInstances(status: InstanceStatus? = null): Result<List<FederatedInstance>>
    suspend fun getInstance(domain: String): Result<FederatedInstance>
    suspend fun blockInstance(instanceId: String): Result<Unit>
    suspend fun removeInstance(instanceId: String): Result<Unit>
    suspend fun createShare(
        itemId: String,
        targetInstance: String,
        targetUserId: String?,
        permissions: List<SharePermission>,
        expiresInDays: Int?,
    ): Result<FederatedShare>
    suspend fun getOutgoingShares(): Result<List<FederatedShare>>
    suspend fun getIncomingShares(status: FederatedShareStatus? = null): Result<List<FederatedShare>>
    suspend fun acceptShare(shareId: String): Result<Unit>
    suspend fun declineShare(shareId: String): Result<Unit>
    suspend fun revokeShare(shareId: String): Result<Unit>
    suspend fun linkIdentity(
        remoteUserId: String,
        remoteInstance: String,
        displayName: String,
    ): Result<FederatedIdentity>
    suspend fun getIdentities(): Result<List<FederatedIdentity>>
    suspend fun unlinkIdentity(identityId: String): Result<Unit>
    suspend fun getActivities(
        instance: String? = null,
        since: Instant? = null,
        limit: Int = 100,
    ): Result<List<FederatedActivity>>
}
