/**
 * Activity Domain Model
 */

package com.vaultstadio.app.domain.model

import kotlinx.datetime.Instant

data class Activity(
    val id: String,
    val type: String,
    val userId: String?,
    val itemId: String?,
    val itemPath: String?,
    val details: String?,
    val createdAt: Instant,
)
