/**
 * Generate File Signature Use Case
 *
 * Application use case for generating a file signature for delta sync.
 */

package com.vaultstadio.api.application.usecase.sync

import arrow.core.Either
import com.vaultstadio.core.domain.model.FileSignature
import com.vaultstadio.core.domain.service.SyncService
import com.vaultstadio.core.exception.StorageException

interface GenerateFileSignatureUseCase {

    suspend operator fun invoke(
        itemId: String,
        versionNumber: Int,
        blockSize: Int,
    ): Either<StorageException, FileSignature>
}

class GenerateFileSignatureUseCaseImpl(
    private val syncService: SyncService,
) : GenerateFileSignatureUseCase {

    override suspend fun invoke(
        itemId: String,
        versionNumber: Int,
        blockSize: Int,
    ): Either<StorageException, FileSignature> =
        syncService.generateFileSignature(itemId, versionNumber, blockSize)
}
