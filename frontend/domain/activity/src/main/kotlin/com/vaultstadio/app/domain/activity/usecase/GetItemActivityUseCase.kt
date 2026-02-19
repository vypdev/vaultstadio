/**
 * Use case for getting activity related to a specific item.
 */

package com.vaultstadio.app.domain.activity.usecase

import com.vaultstadio.app.domain.activity.model.Activity
import com.vaultstadio.app.domain.result.Result

interface GetItemActivityUseCase {
    suspend operator fun invoke(itemId: String, limit: Int = 20): Result<List<Activity>>
}
