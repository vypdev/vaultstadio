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
    val hasMore: Boolean get() = offset.toLong() + items.size < total
    val totalPages: Long
        get() {
            if (limit <= 0) return 0L
            return total / limit + if (total % limit > 0) 1L else 0L
        }
    val currentPage: Long get() = if (limit > 0) offset.toLong() / limit + 1L else 1L
}
