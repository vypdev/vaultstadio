/**
 * Repository interface for activity operations.
 */

package com.vaultstadio.app.domain.activity

import com.vaultstadio.app.domain.activity.model.Activity
import com.vaultstadio.app.domain.result.Result

interface ActivityRepository {
    suspend fun getRecentActivity(limit: Int = 20): Result<List<Activity>>
    suspend fun getItemActivity(itemId: String, limit: Int = 20): Result<List<Activity>>
}
