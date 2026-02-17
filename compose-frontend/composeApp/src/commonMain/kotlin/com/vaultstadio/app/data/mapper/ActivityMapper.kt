/**
 * Activity Mappers
 */

package com.vaultstadio.app.data.mapper

import com.vaultstadio.app.data.dto.activity.ActivityDTO
import com.vaultstadio.app.domain.model.Activity

fun ActivityDTO.toDomain(): Activity = Activity(
    id = id,
    type = type,
    userId = userId,
    itemId = itemId,
    itemPath = itemPath,
    details = details,
    createdAt = createdAt,
)

fun List<ActivityDTO>.toActivityList(): List<Activity> = map { it.toDomain() }
