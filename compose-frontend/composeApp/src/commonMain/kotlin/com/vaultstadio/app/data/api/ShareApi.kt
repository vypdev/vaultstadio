/**
 * Share API
 */

package com.vaultstadio.app.data.api

import com.vaultstadio.app.data.dto.share.CreateShareRequestDTO
import com.vaultstadio.app.data.dto.share.ShareLinkDTO
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.network.BaseApi
import io.ktor.client.HttpClient
import org.koin.core.annotation.Single

@Single
class ShareApi(client: HttpClient) : BaseApi(client) {

    suspend fun getShares(): ApiResult<List<ShareLinkDTO>> =
        get("/api/v1/shares")

    suspend fun getSharesSharedWithMe(): ApiResult<List<ShareLinkDTO>> =
        get("/api/v1/shares/shared-with-me")

    suspend fun createShare(request: CreateShareRequestDTO): ApiResult<ShareLinkDTO> =
        post("/api/v1/shares", request)

    suspend fun deleteShare(shareId: String): ApiResult<Unit> =
        delete("/api/v1/shares/$shareId")
}
