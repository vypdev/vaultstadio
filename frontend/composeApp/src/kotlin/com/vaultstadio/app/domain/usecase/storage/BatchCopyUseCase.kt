/**
 * Batch Copy Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.BatchResult
/**
 * Use case for batch copy operation.
 */
interface BatchCopyUseCase {
    suspend operator fun invoke(itemIds: List<String>, destinationId: String?): Result<BatchResult>
}

class BatchCopyUseCaseImpl(
    private val storageRepository: StorageRepository,
) : BatchCopyUseCase {

    override suspend operator fun invoke(itemIds: List<String>, destinationId: String?): Result<BatchResult> =
        storageRepository.batchCopy(itemIds, destinationId)
}
