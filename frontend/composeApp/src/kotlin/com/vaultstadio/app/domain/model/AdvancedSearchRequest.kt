package com.vaultstadio.app.domain.model

/**
 * Request model for advanced search operations.
 */
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
