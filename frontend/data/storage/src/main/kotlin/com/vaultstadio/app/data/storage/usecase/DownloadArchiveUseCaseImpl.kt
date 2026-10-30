package com.vaultstadio.app.data.storage.usecase

import com.vaultstadio.app.data.network.mapper.toResult
import com.vaultstadio.app.data.storage.service.StorageService
import com.vaultstadio.app.domain.storage.usecase.DownloadArchiveUseCase
import org.koin.core.annotation.Factory

@Factory(binds = [DownloadArchiveUseCase::class])
class DownloadArchiveUseCaseImpl(private val storageService: StorageService) : DownloadArchiveUseCase {
    override suspend fun invoke(itemIds: List<String>) = storageService.downloadArchive(itemIds).toResult()
}
