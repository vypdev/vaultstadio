package com.vaultstadio.app.data.storage.usecase

import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.storage.usecase.GetItemUseCase
import org.koin.core.annotation.Factory

@Factory(binds = [GetItemUseCase::class])
class GetItemUseCaseImpl(
    private val storageRepository: StorageRepository,
) : GetItemUseCase {

    override suspend fun invoke(itemId: String) = storageRepository.getItem(itemId)
}
