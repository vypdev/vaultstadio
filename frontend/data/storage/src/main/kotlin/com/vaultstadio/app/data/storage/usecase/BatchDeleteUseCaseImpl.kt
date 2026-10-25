package com.vaultstadio.app.data.storage.usecase

import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.storage.usecase.BatchDeleteUseCase
import org.koin.core.annotation.Factory

@Factory(binds = [BatchDeleteUseCase::class])
class BatchDeleteUseCaseImpl(
    private val storageRepository: StorageRepository,
) : BatchDeleteUseCase {

    override suspend fun invoke(itemIds: List<String>, permanent: Boolean) =
        storageRepository.batchDelete(itemIds, permanent)
}
