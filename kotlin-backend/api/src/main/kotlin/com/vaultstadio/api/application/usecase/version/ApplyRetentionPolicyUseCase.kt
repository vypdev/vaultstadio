/**
 * Apply Retention Policy Use Case
 *
 * Application use case for applying version retention policy to an item.
 */

package com.vaultstadio.api.application.usecase.version

import arrow.core.Either
import com.vaultstadio.core.domain.model.VersionRetentionPolicy
import com.vaultstadio.core.domain.service.FileVersionService
import com.vaultstadio.core.exception.StorageException

interface ApplyRetentionPolicyUseCase {

    suspend operator fun invoke(
        itemId: String,
        policy: VersionRetentionPolicy,
    ): Either<StorageException, List<String>>
}

class ApplyRetentionPolicyUseCaseImpl(
    private val fileVersionService: FileVersionService,
) : ApplyRetentionPolicyUseCase {

    override suspend fun invoke(
        itemId: String,
        policy: VersionRetentionPolicy,
    ): Either<StorageException, List<String>> =
        fileVersionService.applyRetentionPolicy(itemId, policy)
}
