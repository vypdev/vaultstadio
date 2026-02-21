/**
 * Implementation of CreateShareUseCase.
 */

package com.vaultstadio.app.data.share.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.share.ShareRepository
import com.vaultstadio.app.domain.share.model.ShareLink
import com.vaultstadio.app.domain.share.usecase.CreateShareUseCase

class CreateShareUseCaseImpl(
    private val shareRepository: ShareRepository,
) : CreateShareUseCase {
    override suspend fun invoke(
        itemId: String,
        expiresInDays: Int?,
        password: String?,
        maxDownloads: Int?,
    ): Result<ShareLink> =
        shareRepository.createShare(itemId, expiresInDays, password, maxDownloads)
}
