/**
 * Get Folder Items Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.PaginatedResponse
import com.vaultstadio.app.domain.model.SortField
import com.vaultstadio.app.domain.model.SortOrder
import com.vaultstadio.app.domain.model.StorageItem
import org.koin.core.annotation.Factory

/**
 * Use case for getting folder items.
 */
interface GetFolderItemsUseCase {
    suspend operator fun invoke(
        folderId: String? = null,
        sortBy: SortField = SortField.NAME,
        sortOrder: SortOrder = SortOrder.ASC,
        limit: Int = 100,
        offset: Int = 0,
    ): ApiResult<PaginatedResponse<StorageItem>>
}

@Factory(binds = [GetFolderItemsUseCase::class])
class GetFolderItemsUseCaseImpl(
    private val storageRepository: StorageRepository,
) : GetFolderItemsUseCase {

    override suspend operator fun invoke(
        folderId: String?,
        sortBy: SortField,
        sortOrder: SortOrder,
        limit: Int,
        offset: Int,
    ): ApiResult<PaginatedResponse<StorageItem>> =
        storageRepository.getItems(folderId, sortBy, sortOrder, limit, offset)
}
