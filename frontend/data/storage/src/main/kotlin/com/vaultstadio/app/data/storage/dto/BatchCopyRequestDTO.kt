/**
 * Batch copy request DTO.
 */

package com.vaultstadio.app.data.storage.dto

import kotlinx.serialization.Serializable

@Serializable
data class BatchCopyRequestDTO(
    val itemIds: List<String>,
    val destinationId: String?,
)
