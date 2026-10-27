package com.vaultstadio.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Document state after applying operations.
 *
 * @property itemId Storage item ID
 * @property version Current document version
 * @property content Current document content
 * @property operations List of operations applied
 * @property lastModified Last modification timestamp
 */
@Serializable
data class DocumentState(
    val itemId: String,
    val version: Long,
    val content: String,
    val operations: List<CollaborationOperation> = emptyList(),
    val lastModified: Instant,
)
