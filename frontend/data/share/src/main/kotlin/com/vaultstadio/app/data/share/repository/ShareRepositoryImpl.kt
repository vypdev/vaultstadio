/**
 * Share repository implementation.
 */

package com.vaultstadio.app.data.share.repository

import com.vaultstadio.app.data.network.ApiClientConfig
import com.vaultstadio.app.data.network.apiV1Location
import com.vaultstadio.app.data.network.mapper.toResult
import com.vaultstadio.app.data.share.service.ShareService
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.share.ShareRepository
import com.vaultstadio.app.domain.share.model.ShareLink

class ShareRepositoryImpl(
    private val shareService: ShareService,
    private val config: ApiClientConfig,
) : ShareRepository {

    override suspend fun getMyShares(): Result<List<ShareLink>> =
        shareService.getMyShares().toResult().flatMap(::relocateAll)

    override suspend fun getSharedWithMe(): Result<List<ShareLink>> =
        shareService.getSharedWithMe().toResult().flatMap(::relocateAll)

    override suspend fun createShare(
        itemId: String,
        expiresInDays: Int?,
        password: String?,
        maxDownloads: Int?,
    ): Result<ShareLink> =
        shareService.createShare(itemId, expiresInDays, password, maxDownloads).toResult().flatMap(::relocate)

    override suspend fun deleteShare(shareId: String): Result<Unit> =
        shareService.deleteShare(shareId).toResult()

    private fun relocate(link: ShareLink): Result<ShareLink> =
        if (SHARE_TOKEN.matches(link.token)) {
            Result.Success(link.copy(url = apiV1Location(config.baseUrl, listOf("share", link.token, "view"))))
        } else {
            Result.Error("INVALID_SHARE_RESPONSE", "The server returned an invalid share link.")
        }

    private fun relocateAll(links: List<ShareLink>): Result<List<ShareLink>> {
        val relocated = mutableListOf<ShareLink>()
        for (link in links) {
            when (val result = relocate(link)) {
                is Result.Success -> relocated.add(result.data)
                is Result.Error -> return result
                is Result.NetworkError -> return result
            }
        }
        return Result.Success(relocated)
    }

    private companion object {
        val SHARE_TOKEN = Regex("[A-Za-z0-9_-]{1,64}")
    }
}
