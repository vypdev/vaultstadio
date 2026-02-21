/**
 * Batch error DTO.
 */

package com.vaultstadio.app.data.storage.dto

import kotlinx.serialization.Serializable

@Serializable
data class BatchErrorDTO(
    val itemId: String,
    val error: String,
)
