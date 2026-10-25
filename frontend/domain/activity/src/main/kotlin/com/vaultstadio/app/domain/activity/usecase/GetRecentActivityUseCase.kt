/**
 * Use case for getting recent user activity.
 */

package com.vaultstadio.app.domain.activity.usecase

import com.vaultstadio.app.domain.activity.model.Activity
import com.vaultstadio.app.domain.result.Result

interface GetRecentActivityUseCase {
    suspend operator fun invoke(limit: Int = 20): Result<List<Activity>>
}
