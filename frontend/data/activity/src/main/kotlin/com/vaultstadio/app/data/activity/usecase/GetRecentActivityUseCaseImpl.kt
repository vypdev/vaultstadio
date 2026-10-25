/**
 * Implementation of GetRecentActivityUseCase.
 */

package com.vaultstadio.app.data.activity.usecase

import com.vaultstadio.app.domain.activity.ActivityRepository
import com.vaultstadio.app.domain.activity.usecase.GetRecentActivityUseCase

class GetRecentActivityUseCaseImpl(
    private val activityRepository: ActivityRepository,
) : GetRecentActivityUseCase {

    override suspend fun invoke(limit: Int) = activityRepository.getRecentActivity(limit)
}
