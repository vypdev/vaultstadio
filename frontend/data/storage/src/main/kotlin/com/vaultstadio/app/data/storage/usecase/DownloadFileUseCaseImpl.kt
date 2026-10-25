package com.vaultstadio.app.data.storage.usecase

import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.storage.usecase.DownloadFileUseCase
import org.koin.core.annotation.Factory

@Factory(binds = [DownloadFileUseCase::class])
class DownloadFileUseCaseImpl(
    private val storageRepository: StorageRepository,
) : DownloadFileUseCase {

    override suspend fun invoke(itemId: String) = storageRepository.downloadFile(itemId)

    override fun getDownloadUrl(itemId: String): String = storageRepository.getDownloadUrl(itemId)
}
