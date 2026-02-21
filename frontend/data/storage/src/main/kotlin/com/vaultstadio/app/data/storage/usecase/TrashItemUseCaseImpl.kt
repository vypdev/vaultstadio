package com.vaultstadio.app.data.storage.usecase

import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.storage.usecase.TrashItemUseCase
import org.koin.core.annotation.Factory

@Factory(binds = [TrashItemUseCase::class])
class TrashItemUseCaseImpl(
    private val storageRepository: StorageRepository,
) : TrashItemUseCase {

    override suspend fun invoke(itemId: String) = storageRepository.trashItem(itemId)
}
