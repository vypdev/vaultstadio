package com.vaultstadio.app.data.storage.usecase

import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.storage.usecase.GetStarredUseCase
import org.koin.core.annotation.Factory

@Factory(binds = [GetStarredUseCase::class])
class GetStarredUseCaseImpl(
    private val storageRepository: StorageRepository,
) : GetStarredUseCase {

    override suspend fun invoke() = storageRepository.getStarred()
}
