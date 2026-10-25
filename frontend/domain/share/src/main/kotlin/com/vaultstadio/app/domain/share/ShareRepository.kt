/**
 * Repository interface for share operations.
 */

package com.vaultstadio.app.domain.share

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.share.model.ShareLink

interface ShareRepository {
    suspend fun getMyShares(): Result<List<ShareLink>>
    suspend fun getSharedWithMe(): Result<List<ShareLink>>
    suspend fun createShare(
        itemId: String,
        expiresInDays: Int? = null,
        password: String? = null,
        maxDownloads: Int? = null,
    ): Result<ShareLink>
    suspend fun deleteShare(shareId: String): Result<Unit>
}
