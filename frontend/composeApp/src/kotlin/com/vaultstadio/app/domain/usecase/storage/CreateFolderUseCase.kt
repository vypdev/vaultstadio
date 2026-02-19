/**
 * Create Folder Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.StorageItem
import org.koin.core.annotation.Factory

/**
 * Use case for creating a folder.
 */
interface CreateFolderUseCase {
    suspend operator fun invoke(name: String, parentId: String? = null): Result<StorageItem>
}

@Factory(binds = [CreateFolderUseCase::class])
class CreateFolderUseCaseImpl(
    private val storageRepository: StorageRepository,
) : CreateFolderUseCase {

    override suspend operator fun invoke(name: String, parentId: String?): Result<StorageItem> =
        storageRepository.createFolder(name, parentId)
}
