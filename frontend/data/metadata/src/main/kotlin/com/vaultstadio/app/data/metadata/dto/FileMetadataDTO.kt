/**
 * File metadata DTO.
 */

package com.vaultstadio.app.data.metadata.dto

import kotlinx.serialization.Serializable

@Serializable
data class FileMetadataDTO(
    val itemId: String,
    val metadata: Map<String, String> = emptyMap(),
    val extractedBy: List<String> = emptyList(),
)
