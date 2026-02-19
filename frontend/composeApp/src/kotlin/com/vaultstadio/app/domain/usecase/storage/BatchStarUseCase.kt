/**
 * Batch Star Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.BatchResult
/**
 * Use case for batch star operation.
 */
interface BatchStarUseCase {
    suspend operator fun invoke(itemIds: List<String>, starred: Boolean): Result<BatchResult>
}

class BatchStarUseCaseImpl(
    private val storageRepository: StorageRepository,
) : BatchStarUseCase {

    override suspend operator fun invoke(itemIds: List<String>, starred: Boolean): Result<BatchResult> =
        storageRepository.batchStar(itemIds, starred)
}
