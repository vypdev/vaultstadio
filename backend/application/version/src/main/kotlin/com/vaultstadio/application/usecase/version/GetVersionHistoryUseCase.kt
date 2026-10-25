/**
 * Get Version History Use Case
 *
 * Application use case for retrieving version history of an item.
 */

package com.vaultstadio.application.usecase.version

import arrow.core.Either
import com.vaultstadio.core.domain.model.FileVersionHistory
import com.vaultstadio.core.domain.service.FileVersionService
import com.vaultstadio.domain.common.exception.StorageException

interface GetVersionHistoryUseCase {

    suspend operator fun invoke(itemId: String): Either<StorageException, FileVersionHistory>
}

class GetVersionHistoryUseCaseImpl(
    private val fileVersionService: FileVersionService,
) : GetVersionHistoryUseCase {

    override suspend fun invoke(itemId: String): Either<StorageException, FileVersionHistory> =
        fileVersionService.getHistory(itemId)
}
