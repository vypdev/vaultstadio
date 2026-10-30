package com.vaultstadio.api.routes.metadata

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiError
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.api.dto.PaginatedResponse
import com.vaultstadio.api.dto.SearchRequest
import com.vaultstadio.api.dto.toResponse
import com.vaultstadio.api.routes.storage.SelectionSurface
import com.vaultstadio.api.routes.storage.checkedBodySelection
import com.vaultstadio.api.routes.storage.receiveSelectionRequest
import com.vaultstadio.api.routes.storage.storageSelection
import com.vaultstadio.application.usecase.storage.SearchUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import org.koin.ktor.ext.get as koinGet

internal fun Route.basicSearchRoutes() {
    basicSearchGetRoute()
    basicSearchPostRoute()
}

private fun Route.basicSearchGetRoute() {
    // Basic filename search
    get {
        val searchUseCase: SearchUseCase = call.application.koinGet()
        val user = call.user!!
        val query = call.request.queryParameters["q"] ?: ""
        val selection = call.storageSelection(SelectionSurface.SEARCH)

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

        searchUseCase(query, user.id, selection.page.limit, selection.page.offset).fold(
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
}

private fun Route.basicSearchPostRoute() {
    // Basic search with POST body
    post {
        val searchUseCase: SearchUseCase = call.application.koinGet()
        val user = call.user!!
        val request = call.receiveSelectionRequest<SearchRequest>()
        val selection = checkedBodySelection(request.limit, request.offset)

        searchUseCase(
            request.query,
            user.id,
            selection.limit,
            selection.offset,
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
}
