package com.vaultstadio.app.data.storage.usecase

import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.storage.usecase.ToggleStarUseCase
import org.koin.core.annotation.Factory

@Factory(binds = [ToggleStarUseCase::class])
class ToggleStarUseCaseImpl(
    private val storageRepository: StorageRepository,
) : ToggleStarUseCase {

    override suspend fun invoke(itemId: String) = storageRepository.toggleStar(itemId)
}
