/**
 * Pull changes use case implementation.
 */

package com.vaultstadio.app.data.sync.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.sync.SyncRepository
import com.vaultstadio.app.domain.sync.model.SyncResponse
import com.vaultstadio.app.domain.sync.usecase.PullChangesUseCase

class PullChangesUseCaseImpl(
    private val syncRepository: SyncRepository,
) : PullChangesUseCase {

    override suspend operator fun invoke(
        deviceId: String,
        cursor: String?,
        limit: Int,
        includeDeleted: Boolean,
    ): Result<SyncResponse> = syncRepository.pullChanges(deviceId, cursor, limit, includeDeleted)
}
