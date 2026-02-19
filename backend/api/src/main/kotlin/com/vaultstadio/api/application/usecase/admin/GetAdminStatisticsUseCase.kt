/**
 * Get Admin Statistics Use Case
 */

package com.vaultstadio.api.application.usecase.admin

import arrow.core.Either
import com.vaultstadio.core.domain.model.StorageStatistics
import com.vaultstadio.core.domain.repository.ActivityRepository
import com.vaultstadio.core.exception.StorageException

interface GetAdminStatisticsUseCase {
    suspend operator fun invoke(): Either<StorageException, StorageStatistics>
}

class GetAdminStatisticsUseCaseImpl(
    private val activityRepository: ActivityRepository,
) : GetAdminStatisticsUseCase {
    override suspend fun invoke(): Either<StorageException, StorageStatistics> =
        activityRepository.getStatistics()
}
