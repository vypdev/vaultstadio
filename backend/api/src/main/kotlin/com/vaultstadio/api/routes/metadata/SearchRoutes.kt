/**
 * VaultStadio Search Routes
 *
 * Provides file name search and full-text content search.
 * Integrates with the FullTextSearchPlugin for content-based searches.
 */

package com.vaultstadio.api.routes.metadata

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiError
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.api.dto.PaginatedResponse
import com.vaultstadio.api.dto.SearchRequest
import com.vaultstadio.api.dto.toResponse
import com.vaultstadio.application.usecase.storage.SearchUseCase
import com.vaultstadio.plugins.api.PluginManager
import com.vaultstadio.core.domain.repository.MetadataRepository
import com.vaultstadio.core.domain.service.StorageService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.get as koinGet

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

fun Route.searchRoutes() {
    route("/search") {
        // Basic filename search
        get {
            val searchUseCase: SearchUseCase = call.application.koinGet()
            val user = call.user!!
            val query = call.request.queryParameters["q"] ?: ""
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

            if (query.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("INVALID_QUERY", "Search query cannot be empty"),
                    ),
                )
                return@get
            }

            searchUseCase(query, user.id, limit, offset).fold(
                { error -> throw error },
                { result ->
                    val response = PaginatedResponse(
                        items = result.items.map { it.toResponse() },
                        total = result.total,
                        page = result.currentPage,
                        pageSize = result.limit,
                        totalPages = result.totalPages,
                        hasMore = result.hasMore,
                    )
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(success = true, data = response),
                    )
                },
            )
        }

        // Basic search with POST body
        post {
            val searchUseCase: SearchUseCase = call.application.koinGet()
            val user = call.user!!
            val request = call.receive<SearchRequest>()

            searchUseCase(
                request.query,
                user.id,
                request.limit,
                request.offset,
            ).fold(
                { error -> throw error },
                { result ->
                    val response = PaginatedResponse(
                        items = result.items.map { it.toResponse() },
                        total = result.total,
                        page = result.currentPage,
                        pageSize = result.limit,
                        totalPages = result.totalPages,
                        hasMore = result.hasMore,
                    )
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(success = true, data = response),
                    )
                },
            )
        }

        // Advanced search with filters and full-text content search
        post("/advanced") {
            val storageService: StorageService = call.application.koinGet()
            val pluginManager: PluginManager = call.application.koinGet()
            val user = call.user!!
            val request = call.receive<AdvancedSearchRequest>()

            if (request.query.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("INVALID_QUERY", "Search query cannot be empty"),
                    ),
                )
                return@post
            }

            // If searchContent is enabled, check if full-text search plugin is available
            if (request.searchContent) {
                val isFullTextAvailable = pluginManager.isPluginEnabled("com.vaultstadio.plugins.fulltext-search")

                if (!isFullTextAvailable) {
                    // Full-text search not available, inform user and fallback to filename search
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = mapOf(
                                "type" to "filename",
                                "warning" to "Full-text search plugin not available, falling back to filename search",
                            ),
                        ),
                    )
                    // Continue with filename search below
                }
                // Note: Full-text search via plugin requires invoking through registered endpoints
                // Use POST /api/v1/plugins/com.vaultstadio.plugins.fulltext-search/api/search instead
            }

            // Fallback to filename search
            storageService.search(
                query = request.query,
                userId = user.id,
                limit = request.limit,
                offset = request.offset,
            ).fold(
                { error -> throw error },
                { result ->
                    // Apply additional filters
                    var filteredItems = result.items

                    // Filter by file types
                    request.fileTypes?.let { types ->
                        filteredItems = filteredItems.filter { item ->
                            types.any { type ->
                                item.mimeType?.contains(type, ignoreCase = true) == true
                            }
                        }
                    }

                    // Filter by size
                    request.minSize?.let { minSize ->
                        filteredItems = filteredItems.filter { it.size >= minSize }
                    }
                    request.maxSize?.let { maxSize ->
                        filteredItems = filteredItems.filter { it.size <= maxSize }
                    }

                    // Filter by date
                    request.fromDate?.let { from ->
                        try {
                            val fromInstant = Instant.parse(from)
                            filteredItems = filteredItems.filter { it.createdAt >= fromInstant }
                        } catch (e: Exception) { /* ignore invalid date */ }
                    }
                    request.toDate?.let { to ->
                        try {
                            val toInstant = Instant.parse(to)
                            filteredItems = filteredItems.filter { it.createdAt <= toInstant }
                        } catch (e: Exception) { /* ignore invalid date */ }
                    }

                    val response = PaginatedResponse(
                        items = filteredItems.map { it.toResponse() },
                        total = filteredItems.size.toLong(),
                        page = result.currentPage,
                        pageSize = result.limit,
                        totalPages = (filteredItems.size + result.limit - 1) / result.limit,
                        hasMore = false,
                    )
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(success = true, data = response),
                    )
                },
            )
        }

        // Search by metadata
        get("/by-metadata") {
            val storageService: StorageService = call.application.koinGet()
            val metadataRepository: MetadataRepository = call.application.koinGet()
            val user = call.user!!
            val key = call.request.queryParameters["key"]
            val value = call.request.queryParameters["value"]
            val pluginId = call.request.queryParameters["pluginId"]
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50

            if (key.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(
                        success = false,
                        error = ApiError("INVALID_KEY", "Metadata key is required"),
                    ),
                )
                return@get
            }

            // Search by metadata key/value
            val valuePattern = value ?: "%"

            val searchResult = metadataRepository.searchByKeyValue(
                key = key,
                valuePattern = valuePattern,
                pluginId = pluginId,
                limit = limit,
            )

            searchResult.fold(
                { error -> throw error },
                { metadataList ->
                    // Build results from metadata
                    val results = metadataList.mapNotNull { metadata ->
                        storageService.getItem(metadata.itemId, user.id).fold(
                            { null },
                            { item ->
                                item?.let {
                                    MetadataSearchResult(
                                        itemId = it.id,
                                        itemName = it.name,
                                        itemPath = it.path,
                                        pluginId = metadata.pluginId,
                                        key = metadata.key,
                                        value = metadata.value,
                                    )
                                }
                            },
                        )
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = PaginatedResponse(
                                items = results,
                                total = results.size.toLong(),
                                page = 0,
                                pageSize = limit,
                                totalPages = 1,
                                hasMore = false,
                            ),
                        ),
                    )
                },
            )
        }

        // Get search suggestions (autocomplete)
        get("/suggestions") {
            val storageService: StorageService = call.application.koinGet()
            val user = call.user!!
            val prefix = call.request.queryParameters["prefix"] ?: ""
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10

            if (prefix.length < 2) {
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(success = true, data = emptyList<String>()),
                )
                return@get
            }

            // Search for matching file names
            storageService.search(prefix, user.id, limit, 0).fold(
                { error -> throw error },
                { result ->
                    val suggestions = result.items.map { it.name }.distinct().take(limit)
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(success = true, data = suggestions),
                    )
                },
            )
        }
    }
}
