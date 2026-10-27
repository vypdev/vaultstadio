package com.vaultstadio.api.routes.metadata

import com.vaultstadio.api.config.user
import com.vaultstadio.api.dto.ApiResponse
import com.vaultstadio.api.routes.storage.SelectionSurface
import com.vaultstadio.api.routes.storage.storageSelection
import com.vaultstadio.core.domain.service.StorageService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.ext.get as koinGet

internal fun Route.suggestionRoutes() {
    // Get search suggestions (autocomplete)
    get("/suggestions") {
        val storageService: StorageService = call.application.koinGet()
        val user = call.user!!
        val prefix = call.request.queryParameters["prefix"] ?: ""
        val selection = call.storageSelection(SelectionSurface.SUGGESTIONS)

        if (prefix.length < 2) {
            call.respond(
                HttpStatusCode.OK,
                ApiResponse(success = true, data = emptyList<String>()),
            )
            return@get
        }

        // Search for matching file names
        storageService.search(prefix, user.id, selection.page.limit, 0).fold(
            { error -> throw error },
            { result ->
                val suggestions = result.items.map { it.name }.distinct().take(selection.page.limit)
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(success = true, data = suggestions),
                )
            },
        )
    }
}
