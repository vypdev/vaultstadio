/**
 * Use case for getting file metadata.
 */

package com.vaultstadio.app.domain.metadata.usecase

import com.vaultstadio.app.domain.metadata.model.FileMetadata
import com.vaultstadio.app.domain.result.Result

interface GetFileMetadataUseCase {
    suspend operator fun invoke(itemId: String): Result<FileMetadata>
}
