package com.vaultstadio.app.data.storage.usecase

import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.storage.usecase.BatchMoveUseCase
import org.koin.core.annotation.Factory

@Factory(binds = [BatchMoveUseCase::class])
class BatchMoveUseCaseImpl(
    private val storageRepository: StorageRepository,
) : BatchMoveUseCase {

    override suspend fun invoke(itemIds: List<String>, destinationId: String?) =
        storageRepository.batchMove(itemIds, destinationId)
}
