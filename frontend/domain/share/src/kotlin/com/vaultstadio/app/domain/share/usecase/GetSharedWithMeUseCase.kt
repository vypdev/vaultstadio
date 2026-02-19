/**
 * Use case for getting shares shared with the current user.
 */

package com.vaultstadio.app.domain.share.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.share.model.ShareLink

interface GetSharedWithMeUseCase {
    suspend operator fun invoke(): Result<List<ShareLink>>
}
