/**
 * Get Item Activity Use Case
 */

package com.vaultstadio.app.domain.usecase.activity

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.ActivityRepository
import com.vaultstadio.app.domain.model.Activity
/**
 * Use case for getting activity related to a specific item.
 */
interface GetItemActivityUseCase {
    suspend operator fun invoke(itemId: String, limit: Int = 20): Result<List<Activity>>
}

class GetItemActivityUseCaseImpl(
    private val activityRepository: ActivityRepository,
) : GetItemActivityUseCase {

    override suspend operator fun invoke(itemId: String, limit: Int): Result<List<Activity>> =
        activityRepository.getItemActivity(itemId, limit)
}
