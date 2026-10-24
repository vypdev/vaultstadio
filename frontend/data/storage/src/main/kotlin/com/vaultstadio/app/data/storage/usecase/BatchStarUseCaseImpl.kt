package com.vaultstadio.app.data.storage.usecase

import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.storage.usecase.BatchStarUseCase
import org.koin.core.annotation.Factory

@Factory(binds = [BatchStarUseCase::class])
class BatchStarUseCaseImpl(
    private val storageRepository: StorageRepository,
) : BatchStarUseCase {

    override suspend fun invoke(itemIds: List<String>, starred: Boolean) =
        storageRepository.batchStar(itemIds, starred)
}
