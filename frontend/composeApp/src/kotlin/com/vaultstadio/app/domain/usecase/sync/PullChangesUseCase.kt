/**
 * Pull Changes Use Case
 */

package com.vaultstadio.app.domain.usecase.sync

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.SyncRepository
import com.vaultstadio.app.domain.model.SyncResponse
import org.koin.core.annotation.Factory

/**
 * Use case for pulling sync changes from the server.
 */
interface PullChangesUseCase {
    suspend operator fun invoke(
        deviceId: String,
        cursor: String? = null,
        limit: Int = 1000,
        includeDeleted: Boolean = true,
    ): Result<SyncResponse>
}

@Factory(binds = [PullChangesUseCase::class])
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
