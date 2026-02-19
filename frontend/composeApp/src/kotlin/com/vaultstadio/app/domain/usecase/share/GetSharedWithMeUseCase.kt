/**
 * Get Shared With Me Use Case
 */

package com.vaultstadio.app.domain.usecase.share

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.ShareRepository
import com.vaultstadio.app.domain.model.ShareLink
/**
 * Use case for getting shares shared with the current user.
 */
interface GetSharedWithMeUseCase {
    suspend operator fun invoke(): Result<List<ShareLink>>
}

class GetSharedWithMeUseCaseImpl(
    private val shareRepository: ShareRepository,
) : GetSharedWithMeUseCase {

    override suspend operator fun invoke(): Result<List<ShareLink>> =
        shareRepository.getSharedWithMe()
}
