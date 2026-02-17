/**
 * Batch Delete Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.BatchResult
import org.koin.core.annotation.Factory

/**
 * Use case for batch delete operations on multiple items.
 */
interface BatchDeleteUseCase {
    suspend operator fun invoke(itemIds: List<String>, permanent: Boolean = false): ApiResult<BatchResult>
}

@Factory(binds = [BatchDeleteUseCase::class])
class BatchDeleteUseCaseImpl(
    private val storageRepository: StorageRepository,
) : BatchDeleteUseCase {

    override suspend operator fun invoke(itemIds: List<String>, permanent: Boolean): ApiResult<BatchResult> =
        storageRepository.batchDelete(itemIds, permanent)
}
