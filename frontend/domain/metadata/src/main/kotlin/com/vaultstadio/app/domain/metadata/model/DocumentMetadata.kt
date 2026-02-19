/**
 * Document metadata domain model.
 */

package com.vaultstadio.app.domain.metadata.model

import kotlinx.datetime.Instant

data class DocumentMetadata(
    val title: String? = null,
    val author: String? = null,
    val subject: String? = null,
    val keywords: List<String> = emptyList(),
    val creator: String? = null,
    val producer: String? = null,
    val creationDate: Instant? = null,
    val modificationDate: Instant? = null,
    val pageCount: Int? = null,
    val wordCount: Int? = null,
    val isIndexed: Boolean = false,
    val indexedAt: Instant? = null,
)
