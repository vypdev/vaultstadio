/**
 * Get Recent Activity By Item Use Case
 */

package com.vaultstadio.api.application.usecase.activity

import arrow.core.Either
import com.vaultstadio.core.domain.model.Activity
import com.vaultstadio.core.domain.repository.ActivityRepository
import com.vaultstadio.core.exception.StorageException

interface GetRecentActivityByItemUseCase {
    suspend operator fun invoke(itemId: String, limit: Int = 20): Either<StorageException, List<Activity>>
}

class GetRecentActivityByItemUseCaseImpl(
    private val activityRepository: ActivityRepository,
) : GetRecentActivityByItemUseCase {
    override suspend fun invoke(itemId: String, limit: Int): Either<StorageException, List<Activity>> =
        activityRepository.getRecentByItem(itemId, limit)
}
