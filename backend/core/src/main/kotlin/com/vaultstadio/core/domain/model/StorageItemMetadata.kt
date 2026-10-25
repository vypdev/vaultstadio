/**
 * Extended metadata for a storage item (plugin-attached).
 * Kept in core until domain:metadata is fully populated.
 */

package com.vaultstadio.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class StorageItemMetadata(
    val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val pluginId: String,
    val key: String,
    val value: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
