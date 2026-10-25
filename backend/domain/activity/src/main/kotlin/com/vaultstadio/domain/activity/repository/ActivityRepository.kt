package com.vaultstadio.domain.activity.repository

import arrow.core.Either
import com.vaultstadio.domain.activity.model.Activity
import com.vaultstadio.domain.activity.model.ActivityType
import com.vaultstadio.domain.activity.model.StorageStatistics
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.common.pagination.PagedResult
import com.vaultstadio.domain.common.pagination.SortOrder
import kotlinx.datetime.Instant

data class ActivityQuery(
    val userId: String? = null,
    val itemId: String? = null,
    val types: List<ActivityType>? = null,
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val sortOrder: SortOrder = SortOrder.DESC,
    val offset: Int = 0,
    val limit: Int = 100,
)

interface ActivityRepository {
    suspend fun create(activity: Activity): Either<StorageException, Activity>
    suspend fun query(query: ActivityQuery): Either<StorageException, PagedResult<Activity>>
    suspend fun getRecentByUser(userId: String, limit: Int = 20): Either<StorageException, List<Activity>>
    suspend fun getRecentByItem(itemId: String, limit: Int = 20): Either<StorageException, List<Activity>>
    suspend fun deleteOlderThan(before: Instant): Either<StorageException, Int>
    suspend fun getStatistics(): Either<StorageException, StorageStatistics>
}
