/**
 * Get Folder Items Use Case
 */

package com.vaultstadio.app.domain.storage.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.PaginatedResponse
import com.vaultstadio.app.domain.storage.model.SortField
import com.vaultstadio.app.domain.storage.model.SortOrder
import com.vaultstadio.app.domain.storage.model.StorageItem

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
    ): Result<PaginatedResponse<StorageItem>>
}
