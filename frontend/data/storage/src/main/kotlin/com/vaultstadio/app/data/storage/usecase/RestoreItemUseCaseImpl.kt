package com.vaultstadio.app.data.storage.usecase

import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.storage.usecase.RestoreItemUseCase
import org.koin.core.annotation.Factory

@Factory(binds = [RestoreItemUseCase::class])
class RestoreItemUseCaseImpl(
    private val storageRepository: StorageRepository,
) : RestoreItemUseCase {

    override suspend fun invoke(itemId: String) = storageRepository.restoreItem(itemId)
}
