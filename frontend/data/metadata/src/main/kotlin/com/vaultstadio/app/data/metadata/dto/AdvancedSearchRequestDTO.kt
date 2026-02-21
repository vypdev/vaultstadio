/**
 * Advanced search request DTO.
 */

package com.vaultstadio.app.data.metadata.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class AdvancedSearchRequestDTO(
    val query: String,
    val searchContent: Boolean = false,
    val fileTypes: List<String>? = null,
    val minSize: Long? = null,
    val maxSize: Long? = null,
    @kotlinx.serialization.Contextual
    val fromDate: Instant? = null,
    @kotlinx.serialization.Contextual
    val toDate: Instant? = null,
    val limit: Int = 50,
    val offset: Int = 0,
)
