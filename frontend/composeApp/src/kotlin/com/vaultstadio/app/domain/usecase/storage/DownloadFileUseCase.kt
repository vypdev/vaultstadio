/**
 * Download File Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.StorageRepository
/**
 * Use case for downloading a file.
 */
interface DownloadFileUseCase {
    suspend operator fun invoke(itemId: String): Result<ByteArray>
    fun getDownloadUrl(itemId: String): String
}

class DownloadFileUseCaseImpl(
    private val storageRepository: StorageRepository,
) : DownloadFileUseCase {

    override suspend operator fun invoke(itemId: String): Result<ByteArray> =
        storageRepository.downloadFile(itemId)

    override fun getDownloadUrl(itemId: String): String =
        storageRepository.getDownloadUrl(itemId)
}
