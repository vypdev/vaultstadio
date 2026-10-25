package com.vaultstadio.app.data.storage.usecase

import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.storage.usecase.BatchCopyUseCase
import org.koin.core.annotation.Factory

@Factory(binds = [BatchCopyUseCase::class])
class BatchCopyUseCaseImpl(
    private val storageRepository: StorageRepository,
) : BatchCopyUseCase {

    override suspend fun invoke(itemIds: List<String>, destinationId: String?) =
        storageRepository.batchCopy(itemIds, destinationId)
}
