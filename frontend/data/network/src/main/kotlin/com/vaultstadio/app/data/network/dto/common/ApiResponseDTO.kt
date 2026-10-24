/**
 * Common API Response DTOs
 *
 * Data Transfer Objects for API responses.
 */

package com.vaultstadio.app.data.network.dto.common

import kotlinx.serialization.Serializable

/**
 * Generic API response wrapper.
 */
@Serializable
data class ApiResponseDTO<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiErrorDTO? = null,
)

/**
 * API error DTO.
 */
@Serializable
data class ApiErrorDTO(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null,
)

/**
 * Paginated response DTO.
 */
@Serializable
data class PaginatedResponseDTO<T>(
    val items: List<T>,
    val total: Long,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
    val hasMore: Boolean,
)
