package com.vaultstadio.app.data.storage.usecase

import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.storage.usecase.MoveItemUseCase
import org.koin.core.annotation.Factory

@Factory(binds = [MoveItemUseCase::class])
class MoveItemUseCaseImpl(
    private val storageRepository: StorageRepository,
) : MoveItemUseCase {

    override suspend fun invoke(itemId: String, destinationId: String?, newName: String?) =
        storageRepository.moveItem(itemId, destinationId, newName)
}
