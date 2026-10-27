package com.vaultstadio.api.routes.metadata

import kotlinx.serialization.Serializable

// Shared wire models retain their original package and serialization identity.
/**
 * Full-text search result.
 */
@Serializable
data class FullTextSearchResult(
    val itemId: String,
    val name: String,
    val path: String,
    val mimeType: String?,
    val score: Float,
    val snippet: String,
)

/**
 * Advanced search request.
 */
@Serializable
data class AdvancedSearchRequest(
    val query: String,
    val searchContent: Boolean = false,
    val fileTypes: List<String>? = null,
    val minSize: Long? = null,
    val maxSize: Long? = null,
    val fromDate: String? = null,
    val toDate: String? = null,
    val limit: Int = 50,
    val offset: Int = 0,
)

/**
 * Full-text search result with relevance score and snippet.
 */
@Serializable
data class FullTextSearchResultResponse(
    val itemId: String,
    val name: String,
    val path: String,
    val mimeType: String?,
    val score: Float,
    val snippet: String,
)

/**
 * Metadata search result.
 */
@Serializable
data class MetadataSearchResult(
    val itemId: String,
    val itemName: String,
    val itemPath: String,
    val pluginId: String,
    val key: String,
    val value: String,
)
