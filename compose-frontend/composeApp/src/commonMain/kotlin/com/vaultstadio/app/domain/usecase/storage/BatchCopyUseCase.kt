/**
 * Batch Copy Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.BatchResult
import org.koin.core.annotation.Factory

/**
 * Use case for batch copy operation.
 */
interface BatchCopyUseCase {
    suspend operator fun invoke(itemIds: List<String>, destinationId: String?): ApiResult<BatchResult>
}

@Factory(binds = [BatchCopyUseCase::class])
class BatchCopyUseCaseImpl(
    private val storageRepository: StorageRepository,
) : BatchCopyUseCase {

    override suspend operator fun invoke(itemIds: List<String>, destinationId: String?): ApiResult<BatchResult> =
        storageRepository.batchCopy(itemIds, destinationId)
}
