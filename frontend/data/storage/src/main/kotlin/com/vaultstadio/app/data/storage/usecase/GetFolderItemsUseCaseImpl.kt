package com.vaultstadio.app.data.storage.usecase

import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.storage.model.SortField
import com.vaultstadio.app.domain.storage.model.SortOrder
import com.vaultstadio.app.domain.storage.usecase.GetFolderItemsUseCase
import org.koin.core.annotation.Factory

@Factory(binds = [GetFolderItemsUseCase::class])
class GetFolderItemsUseCaseImpl(
    private val storageRepository: StorageRepository,
) : GetFolderItemsUseCase {

    override suspend fun invoke(
        folderId: String?,
        sortBy: SortField,
        sortOrder: SortOrder,
        limit: Int,
        offset: Int,
    ) = storageRepository.getItems(folderId, sortBy, sortOrder, limit, offset)
}
