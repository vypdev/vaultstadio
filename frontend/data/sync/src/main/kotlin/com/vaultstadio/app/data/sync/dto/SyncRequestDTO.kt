/**
 * Sync pull request DTO.
 */

package com.vaultstadio.app.data.sync.dto

import kotlinx.serialization.Serializable

@Serializable
data class SyncRequestDTO(
    val cursor: String? = null,
    val limit: Int = 1000,
    val includeDeleted: Boolean = true,
)
