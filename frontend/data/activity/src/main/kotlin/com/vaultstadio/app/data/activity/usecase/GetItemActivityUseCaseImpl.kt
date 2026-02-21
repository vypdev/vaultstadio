/**
 * Implementation of GetItemActivityUseCase.
 */

package com.vaultstadio.app.data.activity.usecase

import com.vaultstadio.app.domain.activity.ActivityRepository
import com.vaultstadio.app.domain.activity.usecase.GetItemActivityUseCase

class GetItemActivityUseCaseImpl(
    private val activityRepository: ActivityRepository,
) : GetItemActivityUseCase {

    override suspend fun invoke(itemId: String, limit: Int) =
        activityRepository.getItemActivity(itemId, limit)
}
