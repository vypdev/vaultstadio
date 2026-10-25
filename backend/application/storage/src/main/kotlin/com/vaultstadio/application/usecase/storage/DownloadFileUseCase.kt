/**
 * Download File Use Case
 *
 * Application use case for downloading a file (returns item and stream).
 */

package com.vaultstadio.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.model.StorageItem
import java.io.InputStream

/**
 * Use case for downloading a file. Returns the item metadata and input stream.
 */
interface DownloadFileUseCase {

    suspend operator fun invoke(
        itemId: String,
        userId: String,
    ): Either<StorageException, Pair<StorageItem, InputStream>>
}

/**
 * Default implementation delegating to [StorageService].
 */
class DownloadFileUseCaseImpl(
    private val storageService: StorageService,
) : DownloadFileUseCase {

    override suspend fun invoke(
        itemId: String,
        userId: String,
    ): Either<StorageException, Pair<StorageItem, InputStream>> =
        storageService.downloadFile(itemId, userId)
}
