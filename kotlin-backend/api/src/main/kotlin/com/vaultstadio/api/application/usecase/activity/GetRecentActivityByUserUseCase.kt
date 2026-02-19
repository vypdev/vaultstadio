/**
 * Get Recent Activity By User Use Case
 */

package com.vaultstadio.api.application.usecase.activity

import arrow.core.Either
import com.vaultstadio.core.domain.model.Activity
import com.vaultstadio.core.domain.repository.ActivityRepository
import com.vaultstadio.core.exception.StorageException

interface GetRecentActivityByUserUseCase {
    suspend operator fun invoke(userId: String, limit: Int = 20): Either<StorageException, List<Activity>>
}

class GetRecentActivityByUserUseCaseImpl(
    private val activityRepository: ActivityRepository,
) : GetRecentActivityByUserUseCase {
    override suspend fun invoke(userId: String, limit: Int): Either<StorageException, List<Activity>> =
        activityRepository.getRecentByUser(userId, limit)
}
