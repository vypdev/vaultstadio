/**
 * Implementation of GetMySharesUseCase.
 */

package com.vaultstadio.app.data.share.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.share.ShareRepository
import com.vaultstadio.app.domain.share.model.ShareLink
import com.vaultstadio.app.domain.share.usecase.GetMySharesUseCase

class GetMySharesUseCaseImpl(
    private val shareRepository: ShareRepository,
) : GetMySharesUseCase {
    override suspend fun invoke(): Result<List<ShareLink>> =
        shareRepository.getMyShares()
}
