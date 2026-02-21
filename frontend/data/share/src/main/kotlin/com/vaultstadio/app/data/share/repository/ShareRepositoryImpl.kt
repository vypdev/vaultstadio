/**
 * Share repository implementation.
 */

package com.vaultstadio.app.data.share.repository

import com.vaultstadio.app.data.network.mapper.toResult
import com.vaultstadio.app.data.share.service.ShareService
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.share.ShareRepository
import com.vaultstadio.app.domain.share.model.ShareLink

class ShareRepositoryImpl(
    private val shareService: ShareService,
) : ShareRepository {

    override suspend fun getMyShares(): Result<List<ShareLink>> =
        shareService.getMyShares().toResult()

    override suspend fun getSharedWithMe(): Result<List<ShareLink>> =
        shareService.getSharedWithMe().toResult()

    override suspend fun createShare(
        itemId: String,
        expiresInDays: Int?,
        password: String?,
        maxDownloads: Int?,
    ): Result<ShareLink> =
        shareService.createShare(itemId, expiresInDays, password, maxDownloads).toResult()

    override suspend fun deleteShare(shareId: String): Result<Unit> =
        shareService.deleteShare(shareId).toResult()
}
