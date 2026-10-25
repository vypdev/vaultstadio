/**
 * Use case for deleting a share.
 */

package com.vaultstadio.app.domain.share.usecase

import com.vaultstadio.app.domain.result.Result

interface DeleteShareUseCase {
    suspend operator fun invoke(shareId: String): Result<Unit>
}
