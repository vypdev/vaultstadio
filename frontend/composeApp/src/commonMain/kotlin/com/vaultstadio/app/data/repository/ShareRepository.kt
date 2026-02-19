/**
 * Share Repository
 */

package com.vaultstadio.app.data.repository

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.service.ShareService
import com.vaultstadio.app.domain.model.ShareLink
import org.koin.core.annotation.Single

/**
 * Repository interface for share operations.
 */
interface ShareRepository {
    suspend fun getMyShares(): ApiResult<List<ShareLink>>
    suspend fun getSharedWithMe(): ApiResult<List<ShareLink>>
    suspend fun createShare(
        itemId: String,
        expiresInDays: Int? = null,
        password: String? = null,
        maxDownloads: Int? = null,
    ): ApiResult<ShareLink>
    suspend fun deleteShare(shareId: String): ApiResult<Unit>
}

@Single(binds = [ShareRepository::class])
class ShareRepositoryImpl(
    private val shareService: ShareService,
) : ShareRepository {

    override suspend fun getMyShares(): ApiResult<List<ShareLink>> =
        shareService.getMyShares()

    override suspend fun getSharedWithMe(): ApiResult<List<ShareLink>> =
        shareService.getSharedWithMe()

    override suspend fun createShare(
        itemId: String,
        expiresInDays: Int?,
        password: String?,
        maxDownloads: Int?,
    ): ApiResult<ShareLink> = shareService.createShare(itemId, expiresInDays, password, maxDownloads)

    override suspend fun deleteShare(shareId: String): ApiResult<Unit> =
        shareService.deleteShare(shareId)
}
