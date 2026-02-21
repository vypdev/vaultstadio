/**
 * Storage item DTO.
 */

package com.vaultstadio.app.data.storage.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class StorageItemDTO(
    val id: String,
    val name: String,
    val path: String,
    val type: String,
    val parentId: String?,
    val size: Long,
    val mimeType: String?,
    val visibility: String,
    val isStarred: Boolean,
    val isTrashed: Boolean,
    @kotlinx.serialization.Contextual
    val createdAt: Instant,
    @kotlinx.serialization.Contextual
    val updatedAt: Instant,
    val metadata: Map<String, String>? = null,
)
