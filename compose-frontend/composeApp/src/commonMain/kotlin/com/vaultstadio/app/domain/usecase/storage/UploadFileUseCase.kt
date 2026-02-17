/**
 * Upload File Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.StorageItem
import org.koin.core.annotation.Factory

/**
 * Use case for uploading a file.
 */
interface UploadFileUseCase {
    suspend operator fun invoke(
        fileName: String,
        fileData: ByteArray,
        mimeType: String,
        parentId: String? = null,
        onProgress: (Float) -> Unit = {},
    ): ApiResult<StorageItem>
}

@Factory(binds = [UploadFileUseCase::class])
class UploadFileUseCaseImpl(
    private val storageRepository: StorageRepository,
) : UploadFileUseCase {

    override suspend operator fun invoke(
        fileName: String,
        fileData: ByteArray,
        mimeType: String,
        parentId: String?,
        onProgress: (Float) -> Unit,
    ): ApiResult<StorageItem> =
        storageRepository.uploadFile(fileName, fileData, mimeType, parentId, onProgress)
}
