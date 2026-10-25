/**
 * Paginated response wrapper (admin list).
 */

package com.vaultstadio.app.domain.admin.model

data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Long,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
    val hasMore: Boolean,
)
