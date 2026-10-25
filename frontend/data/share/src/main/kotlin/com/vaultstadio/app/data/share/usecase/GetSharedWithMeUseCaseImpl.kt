/**
 * Implementation of GetSharedWithMeUseCase.
 */

package com.vaultstadio.app.data.share.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.share.ShareRepository
import com.vaultstadio.app.domain.share.model.ShareLink
import com.vaultstadio.app.domain.share.usecase.GetSharedWithMeUseCase

class GetSharedWithMeUseCaseImpl(
    private val shareRepository: ShareRepository,
) : GetSharedWithMeUseCase {
    override suspend fun invoke(): Result<List<ShareLink>> =
        shareRepository.getSharedWithMe()
}
