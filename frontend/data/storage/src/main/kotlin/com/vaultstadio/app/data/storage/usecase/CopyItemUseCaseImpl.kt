package com.vaultstadio.app.data.storage.usecase

import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.storage.usecase.CopyItemUseCase
import org.koin.core.annotation.Factory

@Factory(binds = [CopyItemUseCase::class])
class CopyItemUseCaseImpl(
    private val storageRepository: StorageRepository,
) : CopyItemUseCase {

    override suspend fun invoke(itemId: String, destinationId: String?, newName: String?) =
        storageRepository.copyItem(itemId, destinationId, newName)
}
