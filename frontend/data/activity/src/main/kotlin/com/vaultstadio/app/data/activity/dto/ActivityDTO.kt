/**
 * Activity Data Transfer Objects
 */

package com.vaultstadio.app.data.activity.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ActivityDTO(
    val id: String,
    val type: String,
    val userId: String?,
    val itemId: String?,
    val itemPath: String?,
    val details: String?,
    @kotlinx.serialization.Contextual
    val createdAt: Instant,
)
