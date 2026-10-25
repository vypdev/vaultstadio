package com.vaultstadio.app.data.storage.usecase

import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.storage.usecase.EmptyTrashUseCase
import org.koin.core.annotation.Factory

@Factory(binds = [EmptyTrashUseCase::class])
class EmptyTrashUseCaseImpl(
    private val storageRepository: StorageRepository,
) : EmptyTrashUseCase {

    override suspend fun invoke() = storageRepository.emptyTrash()
}
