/**
 * Use case for creating a share link.
 */

package com.vaultstadio.app.domain.share.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.share.model.ShareLink

interface CreateShareUseCase {
    suspend operator fun invoke(
        itemId: String,
        expiresInDays: Int? = null,
        password: String? = null,
        maxDownloads: Int? = null,
    ): Result<ShareLink>
}
