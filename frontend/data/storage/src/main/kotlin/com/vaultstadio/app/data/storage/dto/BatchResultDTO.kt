/**
 * Batch result DTO.
 */

package com.vaultstadio.app.data.storage.dto

import kotlinx.serialization.Serializable

@Serializable
data class BatchResultDTO(
    val successful: Int,
    val failed: Int,
    val errors: List<BatchErrorDTO> = emptyList(),
)
