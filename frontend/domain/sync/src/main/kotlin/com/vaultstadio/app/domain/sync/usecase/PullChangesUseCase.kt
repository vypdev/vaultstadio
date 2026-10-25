/**
 * Use case for pulling sync changes from the server.
 */

package com.vaultstadio.app.domain.sync.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.sync.model.SyncResponse

interface PullChangesUseCase {
    suspend operator fun invoke(
        deviceId: String,
        cursor: String? = null,
        limit: Int = 1000,
        includeDeleted: Boolean = true,
    ): Result<SyncResponse>
}
