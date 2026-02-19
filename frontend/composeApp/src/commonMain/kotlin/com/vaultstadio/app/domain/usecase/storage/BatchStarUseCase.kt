/**
 * Batch Star Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.BatchResult
import org.koin.core.annotation.Factory

/**
 * Use case for batch star operation.
 */
interface BatchStarUseCase {
    suspend operator fun invoke(itemIds: List<String>, starred: Boolean): ApiResult<BatchResult>
}

@Factory(binds = [BatchStarUseCase::class])
class BatchStarUseCaseImpl(
    private val storageRepository: StorageRepository,
) : BatchStarUseCase {

    override suspend operator fun invoke(itemIds: List<String>, starred: Boolean): ApiResult<BatchResult> =
        storageRepository.batchStar(itemIds, starred)
}
