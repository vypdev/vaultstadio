/**
 * Get My Shares Use Case
 */

package com.vaultstadio.app.domain.usecase.share

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.ShareRepository
import com.vaultstadio.app.domain.model.ShareLink
import org.koin.core.annotation.Factory

/**
 * Use case for getting shares created by the current user.
 */
interface GetMySharesUseCase {
    suspend operator fun invoke(): Result<List<ShareLink>>
}

@Factory(binds = [GetMySharesUseCase::class])
class GetMySharesUseCaseImpl(
    private val shareRepository: ShareRepository,
) : GetMySharesUseCase {

    override suspend operator fun invoke(): Result<List<ShareLink>> =
        shareRepository.getMyShares()
}
