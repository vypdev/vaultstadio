/**
 * Empty Trash Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.BatchResult
import org.koin.core.annotation.Factory

/**
 * Use case for emptying the trash.
 */
interface EmptyTrashUseCase {
    suspend operator fun invoke(): Result<BatchResult>
}

@Factory(binds = [EmptyTrashUseCase::class])
class EmptyTrashUseCaseImpl(
    private val storageRepository: StorageRepository,
) : EmptyTrashUseCase {

    override suspend operator fun invoke(): Result<BatchResult> =
        storageRepository.emptyTrash()
}
