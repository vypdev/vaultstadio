/**
 * Batch delete request DTO.
 */

package com.vaultstadio.app.data.storage.dto

import kotlinx.serialization.Serializable

@Serializable
data class BatchDeleteRequestDTO(
    val itemIds: List<String>,
    val permanent: Boolean = false,
)
