/**
 * Batch Delete Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.BatchResult
/**
 * Use case for batch delete operations on multiple items.
 */
interface BatchDeleteUseCase {
    suspend operator fun invoke(itemIds: List<String>, permanent: Boolean = false): Result<BatchResult>
}

class BatchDeleteUseCaseImpl(
    private val storageRepository: StorageRepository,
) : BatchDeleteUseCase {

    override suspend operator fun invoke(itemIds: List<String>, permanent: Boolean): Result<BatchResult> =
        storageRepository.batchDelete(itemIds, permanent)
}
