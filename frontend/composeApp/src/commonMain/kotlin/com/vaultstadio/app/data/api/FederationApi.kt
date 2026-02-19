/**
 * Federation API
 */

package com.vaultstadio.app.data.api

import com.vaultstadio.app.data.dto.federation.CreateFederatedShareRequestDTO
import com.vaultstadio.app.data.dto.federation.FederatedActivityDTO
import com.vaultstadio.app.data.dto.federation.FederatedIdentityDTO
import com.vaultstadio.app.data.dto.federation.FederatedInstanceDTO
import com.vaultstadio.app.data.dto.federation.FederatedShareDTO
import com.vaultstadio.app.data.dto.federation.LinkIdentityRequestDTO
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.network.BaseApi
import io.ktor.client.HttpClient
import kotlinx.datetime.Instant
import org.koin.core.annotation.Single

@Single
class FederationApi(client: HttpClient) : BaseApi(client) {
    suspend fun requestFederation(targetDomain: String, message: String?): ApiResult<FederatedInstanceDTO> {
        val body = mutableMapOf("targetDomain" to targetDomain)
        message?.let { body["message"] = it }
        return post("/api/v1/federation/instances/request", body)
    }
    suspend fun getInstances(status: String?): ApiResult<List<FederatedInstanceDTO>> {
        val params = status?.let { mapOf("status" to it) } ?: emptyMap()
        return get("/api/v1/federation/instances", params)
    }
    suspend fun getInstance(
        domain: String,
    ): ApiResult<FederatedInstanceDTO> = get("/api/v1/federation/instances/$domain")
    suspend fun blockInstance(
        instanceId: String,
    ): ApiResult<Unit> = postNoBody("/api/v1/federation/instances/$instanceId/block")
    suspend fun removeInstance(instanceId: String): ApiResult<Unit> = delete("/api/v1/federation/instances/$instanceId")
    suspend fun createShare(
        request: CreateFederatedShareRequestDTO,
    ): ApiResult<FederatedShareDTO> = post("/api/v1/federation/shares", request)
    suspend fun getOutgoingShares(): ApiResult<List<FederatedShareDTO>> = get("/api/v1/federation/shares/outgoing")
    suspend fun getIncomingShares(status: String?): ApiResult<List<FederatedShareDTO>> {
        val params = status?.let { mapOf("status" to it) } ?: emptyMap()
        return get("/api/v1/federation/shares/incoming", params)
    }
    suspend fun acceptShare(shareId: String): ApiResult<Unit> = postNoBody("/api/v1/federation/shares/$shareId/accept")
    suspend fun declineShare(
        shareId: String,
    ): ApiResult<Unit> = postNoBody("/api/v1/federation/shares/$shareId/decline")
    suspend fun revokeShare(shareId: String): ApiResult<Unit> = postNoBody("/api/v1/federation/shares/$shareId/revoke")
    suspend fun linkIdentity(
        request: LinkIdentityRequestDTO,
    ): ApiResult<FederatedIdentityDTO> = post("/api/v1/federation/identities", request)
    suspend fun getIdentities(): ApiResult<List<FederatedIdentityDTO>> = get("/api/v1/federation/identities")
    suspend fun unlinkIdentity(
        identityId: String,
    ): ApiResult<Unit> = delete("/api/v1/federation/identities/$identityId")
    suspend fun getActivities(instance: String?, since: Instant?, limit: Int): ApiResult<List<FederatedActivityDTO>> {
        val params = mutableMapOf<String, String>("limit" to limit.toString())
        instance?.let { params["instance"] = it }
        since?.let { params["since"] = it.toString() }
        return get("/api/v1/federation/activities", params)
    }
}
