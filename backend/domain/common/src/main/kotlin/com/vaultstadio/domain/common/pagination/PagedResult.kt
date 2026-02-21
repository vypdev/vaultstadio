/**
 * Shared pagination types for domain repositories.
 */

package com.vaultstadio.domain.common.pagination

/**
 * Result of a paginated query.
 */
data class PagedResult<T>(
    val items: List<T>,
    val total: Long,
    val offset: Int,
    val limit: Int,
) {
    val hasMore: Boolean get() = offset + items.size < total
    val totalPages: Int get() = if (limit > 0) ((total + limit - 1) / limit).toInt() else 0
    val currentPage: Int get() = if (limit > 0) (offset / limit) + 1 else 1
}
