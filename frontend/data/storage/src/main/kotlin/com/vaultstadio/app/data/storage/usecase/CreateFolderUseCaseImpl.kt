package com.vaultstadio.app.data.storage.usecase

import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.storage.usecase.CreateFolderUseCase
import org.koin.core.annotation.Factory

@Factory(binds = [CreateFolderUseCase::class])
class CreateFolderUseCaseImpl(
    private val storageRepository: StorageRepository,
) : CreateFolderUseCase {

    override suspend fun invoke(name: String, parentId: String?) =
        storageRepository.createFolder(name, parentId)
}
