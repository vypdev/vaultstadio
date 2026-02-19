/**
 * Create Folder Use Case
 */

package com.vaultstadio.app.domain.storage.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.StorageItem

/**
 * Use case for creating a folder.
 */
interface CreateFolderUseCase {
    suspend operator fun invoke(name: String, parentId: String? = null): Result<StorageItem>
}
