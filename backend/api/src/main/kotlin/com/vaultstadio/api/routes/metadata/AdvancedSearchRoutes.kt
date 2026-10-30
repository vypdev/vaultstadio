package com.vaultstadio.api.routes.metadata

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiError
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.api.dto.PaginatedResponse
import com.vaultstadio.api.dto.toResponse
import com.vaultstadio.api.routes.storage.checkedBodySelection
import com.vaultstadio.api.routes.storage.receiveSelectionRequest
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.domain.common.pagination.PagedResult
import com.vaultstadio.domain.storage.model.StorageItem
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.koin.ktor.ext.get as koinGet

internal fun Route.advancedSearchRoutes() {
    post("/advanced") {
        val request = call.receiveSelectionRequest<AdvancedSearchRequest>()
        val selection = checkedBodySelection(request.limit, request.offset)
        if (request.query.isBlank()) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(false, error = ApiError("INVALID_QUERY", "Search query cannot be empty")),
            )
            return@post
        }
        val dates = request.searchDates()
        if (request.searchContent) {
            call.respond(
                HttpStatusCode.NotImplemented,
                ApiResponse<Unit>(
                    false,
                    error = ApiError("CONTENT_SEARCH_UNAVAILABLE", "Content search is not implemented on this route"),
                ),
            )
            return@post
        }
        val storage: StorageService = call.application.koinGet()
        storage.search(request.query, call.user!!.id, selection.limit, selection.offset).fold(
            { throw it },
            { call.respondAdvancedProjection(it, request, dates) },
        )
    }
}

private suspend fun ApplicationCall.respondAdvancedProjection(
    result: PagedResult<StorageItem>,
    request: AdvancedSearchRequest,
    dates: SearchDateBounds,
) {
    // Global predicate-before-pagination semantics remain the explicit follow-up in issue115.
    val items = request.filterProjection(result.items, dates)
    val response = PaginatedResponse(
        items.map { it.toResponse() },
        items.size.toLong(),
        result.currentPage,
        result.limit,
        (items.size.toLong() + result.limit - 1) / result.limit,
        false,
    )
    respond(HttpStatusCode.OK, ApiResponse(success = true, data = response))
}
