/**
 * Metadata search result item.
 */

package com.vaultstadio.app.domain.metadata.model

data class MetadataSearchResult(
    val itemId: String,
    val itemName: String,
    val itemPath: String,
    val pluginId: String,
    val key: String,
    val value: String,
)
