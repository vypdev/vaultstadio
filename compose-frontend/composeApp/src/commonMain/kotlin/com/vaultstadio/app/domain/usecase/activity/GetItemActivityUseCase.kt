/**
 * Get Item Activity Use Case
 */

package com.vaultstadio.app.domain.usecase.activity

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.ActivityRepository
import com.vaultstadio.app.domain.model.Activity
import org.koin.core.annotation.Factory

/**
 * Use case for getting activity related to a specific item.
 */
interface GetItemActivityUseCase {
    suspend operator fun invoke(itemId: String, limit: Int = 20): ApiResult<List<Activity>>
}

@Factory(binds = [GetItemActivityUseCase::class])
class GetItemActivityUseCaseImpl(
    private val activityRepository: ActivityRepository,
) : GetItemActivityUseCase {

    override suspend operator fun invoke(itemId: String, limit: Int): ApiResult<List<Activity>> =
        activityRepository.getItemActivity(itemId, limit)
}
