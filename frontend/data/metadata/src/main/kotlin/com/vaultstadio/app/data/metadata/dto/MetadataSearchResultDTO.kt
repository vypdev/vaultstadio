/**
 * Metadata search result DTO.
 */

package com.vaultstadio.app.data.metadata.dto

import kotlinx.serialization.Serializable

@Serializable
data class MetadataSearchResultDTO(
    val itemId: String,
    val itemName: String,
    val itemPath: String,
    val pluginId: String,
    val key: String,
    val value: String,
)
