/**
 * Get Admin Statistics Use Case
 */

package com.vaultstadio.application.usecase.admin

import arrow.core.Either
import com.vaultstadio.domain.activity.model.StorageStatistics
import com.vaultstadio.domain.activity.repository.ActivityRepository
import com.vaultstadio.domain.common.exception.StorageException

interface GetAdminStatisticsUseCase {
    suspend operator fun invoke(): Either<StorageException, StorageStatistics>
}

class GetAdminStatisticsUseCaseImpl(
    private val activityRepository: ActivityRepository,
) : GetAdminStatisticsUseCase {
    override suspend fun invoke(): Either<StorageException, StorageStatistics> =
        activityRepository.getStatistics()
}
