/**
 * Use case for getting shares created by the current user.
 */

package com.vaultstadio.app.domain.share.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.share.model.ShareLink

interface GetMySharesUseCase {
    suspend operator fun invoke(): Result<List<ShareLink>>
}
