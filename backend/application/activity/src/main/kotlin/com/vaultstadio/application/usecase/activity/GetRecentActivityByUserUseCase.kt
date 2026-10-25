/**
 * Get Recent Activity By User Use Case
 */

package com.vaultstadio.application.usecase.activity

import arrow.core.Either
import com.vaultstadio.domain.activity.model.Activity
import com.vaultstadio.domain.activity.repository.ActivityRepository
import com.vaultstadio.domain.common.exception.StorageException

interface GetRecentActivityByUserUseCase {
    suspend operator fun invoke(userId: String, limit: Int = 20): Either<StorageException, List<Activity>>
}

class GetRecentActivityByUserUseCaseImpl(
    private val activityRepository: ActivityRepository,
) : GetRecentActivityByUserUseCase {
    override suspend fun invoke(userId: String, limit: Int): Either<StorageException, List<Activity>> =
        activityRepository.getRecentByUser(userId, limit)
}
