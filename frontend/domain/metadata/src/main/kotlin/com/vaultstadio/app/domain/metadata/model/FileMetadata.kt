/**
 * File metadata domain model.
 */

package com.vaultstadio.app.domain.metadata.model

data class FileMetadata(
    val itemId: String,
    val metadata: Map<String, String> = emptyMap(),
    val extractedBy: List<String> = emptyList(),
)
