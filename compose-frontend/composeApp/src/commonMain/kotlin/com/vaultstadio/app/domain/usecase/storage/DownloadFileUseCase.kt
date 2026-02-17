/**
 * Download File Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.StorageRepository
import org.koin.core.annotation.Factory

/**
 * Use case for downloading a file.
 */
interface DownloadFileUseCase {
    suspend operator fun invoke(itemId: String): ApiResult<ByteArray>
    fun getDownloadUrl(itemId: String): String
}

@Factory(binds = [DownloadFileUseCase::class])
class DownloadFileUseCaseImpl(
    private val storageRepository: StorageRepository,
) : DownloadFileUseCase {

    override suspend operator fun invoke(itemId: String): ApiResult<ByteArray> =
        storageRepository.downloadFile(itemId)

    override fun getDownloadUrl(itemId: String): String =
        storageRepository.getDownloadUrl(itemId)
}
