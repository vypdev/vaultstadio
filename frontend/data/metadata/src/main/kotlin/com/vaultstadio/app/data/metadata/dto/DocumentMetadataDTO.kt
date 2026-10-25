/**
 * Document metadata DTO.
 */

package com.vaultstadio.app.data.metadata.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class DocumentMetadataDTO(
    val title: String? = null,
    val author: String? = null,
    val subject: String? = null,
    val keywords: List<String> = emptyList(),
    val creator: String? = null,
    val producer: String? = null,
    @kotlinx.serialization.Contextual
    val creationDate: Instant? = null,
    @kotlinx.serialization.Contextual
    val modificationDate: Instant? = null,
    val pageCount: Int? = null,
    val wordCount: Int? = null,
    val isIndexed: Boolean = false,
    @kotlinx.serialization.Contextual
    val indexedAt: Instant? = null,
)
