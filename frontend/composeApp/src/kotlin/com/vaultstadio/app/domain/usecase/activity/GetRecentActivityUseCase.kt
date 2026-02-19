/**
 * Get Recent Activity Use Case
 */

package com.vaultstadio.app.domain.usecase.activity

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.ActivityRepository
import com.vaultstadio.app.domain.model.Activity
/**
 * Use case for getting recent user activity.
 */
interface GetRecentActivityUseCase {
    suspend operator fun invoke(limit: Int = 20): Result<List<Activity>>
}

class GetRecentActivityUseCaseImpl(
    private val activityRepository: ActivityRepository,
) : GetRecentActivityUseCase {

    override suspend operator fun invoke(limit: Int): Result<List<Activity>> =
        activityRepository.getRecentActivity(limit)
}
