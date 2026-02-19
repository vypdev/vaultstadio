/**
 * Batch Move Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.BatchResult
import org.koin.core.annotation.Factory

/**
 * Use case for batch move operation.
 */
interface BatchMoveUseCase {
    suspend operator fun invoke(itemIds: List<String>, destinationId: String?): Result<BatchResult>
}

@Factory(binds = [BatchMoveUseCase::class])
class BatchMoveUseCaseImpl(
    private val storageRepository: StorageRepository,
) : BatchMoveUseCase {

    override suspend operator fun invoke(itemIds: List<String>, destinationId: String?): Result<BatchResult> =
        storageRepository.batchMove(itemIds, destinationId)
}
