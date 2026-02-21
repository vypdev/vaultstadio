package com.vaultstadio.app.data.storage.usecase

import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.storage.usecase.DeleteItemUseCase
import org.koin.core.annotation.Factory

@Factory(binds = [DeleteItemUseCase::class])
class DeleteItemUseCaseImpl(
    private val storageRepository: StorageRepository,
) : DeleteItemUseCase {

    override suspend fun invoke(itemId: String) =
        storageRepository.deleteItemPermanently(itemId)
}
