/**
 * Share service – delegates to ShareApi and maps DTOs to domain.
 */

package com.vaultstadio.app.data.share.service

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.share.api.ShareApi
import com.vaultstadio.app.data.share.dto.CreateShareRequestDTO
import com.vaultstadio.app.data.share.mapper.createShareRequestDTO
import com.vaultstadio.app.data.share.mapper.toDomain
import com.vaultstadio.app.data.share.mapper.toShareList
import com.vaultstadio.app.domain.share.model.ShareLink

class ShareService(private val shareApi: ShareApi) {

    suspend fun getMyShares(): ApiResult<List<ShareLink>> =
        shareApi.getShares().map { it.toShareList() }

    suspend fun getSharedWithMe(): ApiResult<List<ShareLink>> =
        shareApi.getSharesSharedWithMe().map { it.toShareList() }

    suspend fun createShare(
        itemId: String,
        expiresInDays: Int? = null,
        password: String? = null,
        maxDownloads: Int? = null,
    ): ApiResult<ShareLink> =
        shareApi.createShare(
            createShareRequestDTO(itemId, expiresInDays, password, maxDownloads),
        ).map { it.toDomain() }

    suspend fun deleteShare(shareId: String): ApiResult<Unit> =
        shareApi.deleteShare(shareId)
}
