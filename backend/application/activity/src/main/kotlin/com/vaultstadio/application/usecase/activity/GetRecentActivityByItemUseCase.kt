/**
 * Get Recent Activity By Item Use Case
 */

package com.vaultstadio.application.usecase.activity

import arrow.core.Either
import com.vaultstadio.domain.activity.model.Activity
import com.vaultstadio.domain.activity.repository.ActivityRepository
import com.vaultstadio.domain.common.exception.StorageException

interface GetRecentActivityByItemUseCase {
    suspend operator fun invoke(itemId: String, limit: Int = 20): Either<StorageException, List<Activity>>
}

class GetRecentActivityByItemUseCaseImpl(
    private val activityRepository: ActivityRepository,
) : GetRecentActivityByItemUseCase {
    override suspend fun invoke(itemId: String, limit: Int): Either<StorageException, List<Activity>> =
        activityRepository.getRecentByItem(itemId, limit)
}
