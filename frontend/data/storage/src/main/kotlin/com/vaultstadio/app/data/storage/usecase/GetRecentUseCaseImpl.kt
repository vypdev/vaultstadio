package com.vaultstadio.app.data.storage.usecase

import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.storage.usecase.GetRecentUseCase
import org.koin.core.annotation.Factory

@Factory(binds = [GetRecentUseCase::class])
class GetRecentUseCaseImpl(
    private val storageRepository: StorageRepository,
) : GetRecentUseCase {

    override suspend fun invoke(limit: Int) = storageRepository.getRecent(limit)
}
