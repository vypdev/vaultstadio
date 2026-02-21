/**
 * Activity Mappers
 */

package com.vaultstadio.app.data.activity.mapper

import com.vaultstadio.app.data.activity.dto.ActivityDTO
import com.vaultstadio.app.domain.activity.model.Activity

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
