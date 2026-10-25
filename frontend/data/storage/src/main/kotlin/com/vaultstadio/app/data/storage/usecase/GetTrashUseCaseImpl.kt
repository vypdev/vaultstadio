package com.vaultstadio.app.data.storage.usecase

import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.storage.usecase.GetTrashUseCase
import org.koin.core.annotation.Factory

@Factory(binds = [GetTrashUseCase::class])
class GetTrashUseCaseImpl(
    private val storageRepository: StorageRepository,
) : GetTrashUseCase {

    override suspend fun invoke() = storageRepository.getTrash()
}
