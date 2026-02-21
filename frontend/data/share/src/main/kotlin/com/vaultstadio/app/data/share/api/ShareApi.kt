/**
 * Share API – share-related HTTP calls.
 */

package com.vaultstadio.app.data.share.api

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.network.BaseApi
import com.vaultstadio.app.data.share.dto.CreateShareRequestDTO
import com.vaultstadio.app.data.share.dto.ShareLinkDTO
import io.ktor.client.HttpClient

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
