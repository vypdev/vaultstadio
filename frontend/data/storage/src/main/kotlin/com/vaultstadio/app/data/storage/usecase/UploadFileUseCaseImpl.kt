package com.vaultstadio.app.data.storage.usecase

import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.storage.usecase.UploadFileUseCase
import org.koin.core.annotation.Factory

@Factory(binds = [UploadFileUseCase::class])
class UploadFileUseCaseImpl(
    private val storageRepository: StorageRepository,
) : UploadFileUseCase {

    override suspend fun invoke(
        fileName: String,
        fileData: ByteArray,
        mimeType: String,
        parentId: String?,
        onProgress: (Float) -> Unit,
    ) = storageRepository.uploadFile(fileName, fileData, mimeType, parentId, onProgress)
}
