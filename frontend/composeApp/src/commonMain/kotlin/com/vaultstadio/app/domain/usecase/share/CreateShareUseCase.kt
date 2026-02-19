/**
 * Create Share Use Case
 */

package com.vaultstadio.app.domain.usecase.share

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.ShareRepository
import com.vaultstadio.app.domain.model.ShareLink
import org.koin.core.annotation.Factory

/**
 * Use case for creating a share link.
 */
interface CreateShareUseCase {
    suspend operator fun invoke(
        itemId: String,
        expiresInDays: Int? = null,
        password: String? = null,
        maxDownloads: Int? = null,
    ): ApiResult<ShareLink>
}

@Factory(binds = [CreateShareUseCase::class])
class CreateShareUseCaseImpl(
    private val shareRepository: ShareRepository,
) : CreateShareUseCase {

    override suspend operator fun invoke(
        itemId: String,
        expiresInDays: Int?,
        password: String?,
        maxDownloads: Int?,
    ): ApiResult<ShareLink> = shareRepository.createShare(itemId, expiresInDays, password, maxDownloads)
}
