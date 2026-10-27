/**
 * Paginated response wrapper (admin list).
 */

package com.vaultstadio.app.domain.admin.model

data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Long,
    val page: Long,
    val pageSize: Int,
    val totalPages: Long,
    val hasMore: Boolean,
)
