package com.vaultstadio.app.data.storage.usecase

import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.storage.usecase.RenameItemUseCase
import org.koin.core.annotation.Factory

@Factory(binds = [RenameItemUseCase::class])
class RenameItemUseCaseImpl(
    private val storageRepository: StorageRepository,
) : RenameItemUseCase {

    override suspend fun invoke(itemId: String, newName: String) =
        storageRepository.renameItem(itemId, newName)
}
