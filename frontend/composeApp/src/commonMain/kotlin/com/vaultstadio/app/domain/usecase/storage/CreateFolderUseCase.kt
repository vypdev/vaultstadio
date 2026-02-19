/**
 * Create Folder Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.StorageItem
import org.koin.core.annotation.Factory

/**
 * Use case for creating a folder.
 */
interface CreateFolderUseCase {
    suspend operator fun invoke(name: String, parentId: String? = null): ApiResult<StorageItem>
}

@Factory(binds = [CreateFolderUseCase::class])
class CreateFolderUseCaseImpl(
    private val storageRepository: StorageRepository,
) : CreateFolderUseCase {

    override suspend operator fun invoke(name: String, parentId: String?): ApiResult<StorageItem> =
        storageRepository.createFolder(name, parentId)
}
