/**
 * Batch star request DTO.
 */

package com.vaultstadio.app.data.storage.dto

import kotlinx.serialization.Serializable

@Serializable
data class BatchStarRequestDTO(
    val itemIds: List<String>,
    val starred: Boolean,
)
